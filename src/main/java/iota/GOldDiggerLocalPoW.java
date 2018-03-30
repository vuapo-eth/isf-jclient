package iota;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import cfb.pearldiver.PearlDiver;
import isf.Configs;
import isf.FileManager;
import isf.logic.ObjectWrapper;
import isf.P;
import isf.ui.R;
import isf.ui.UIManager;
import jota.IotaLocalPoW;
import jota.utils.Converter;

public class GOldDiggerLocalPoW implements IotaLocalPoW {

    // logging
    private static final UIManager UIM = new UIManager("GOldDggr");

    // stats
	private static int amountPoW = 0;
	private static long totalTimePoW = 0;

	// object wrappers for thread communication
	private static final ObjectWrapper powTrytes = new ObjectWrapper(null);
	private static final ObjectWrapper powSuccess = new ObjectWrapper(false);
	private static final ObjectWrapper scannerOpen = new ObjectWrapper(false);

	// pow
	private static boolean goPowAvailable = false;
    private static final PearlDiver PEARL_DIVER = new PearlDiver();
    private static final File POW_FILE = new File(determinePowFileName());

    @Override
    public String performPoW(String trytes, int minWeightMagnitude) {
        long timeStarted = System.currentTimeMillis();
        String nonced = goPowAvailable ? goPow(trytes) : javaPow(trytes, minWeightMagnitude);
        totalTimePoW += System.currentTimeMillis() - timeStarted;
        amountPoW++;
        return nonced;
    }

    /**
     * performs proof-of-work on trytes using java PearlDiver implementation (low performance)
     * @param preparedTrytes trytes before pow
     * @return trytes after pow
     * */
    private String javaPow(String preparedTrytes, int minWeightMagnitude) {
        int[] trits = Converter.trits(preparedTrytes);
        if (!PEARL_DIVER.search(trits, minWeightMagnitude, Configs.getInt(P.POW_CORES)))
            throw new IllegalStateException(R.STR.getString("pow_abort"));
        return Converter.trytes(trits);
    }

    /**
     * performs proof-of-work on trytes using giota pow implementation (high performance)
     * @param preparedTrytes trytes before pow
     * @return trytes after pow
     * */
    private static String goPow(String preparedTrytes) {
        powTrytes.o = preparedTrytes;
        powSuccess.o = false;

        synchronized (powTrytes) {
            powTrytes.notify();
            try { powTrytes.wait(); } catch (InterruptedException e) { }
        }
        return (boolean) powSuccess.o ? (String) powTrytes.o : null;
    }

    public static void downloadPowIfNonExistent() {
    	
    	if(POW_FILE.exists()) return;

    	boolean goModuleWantedByUser = UIM.askForBoolean(R.STR.getString("pow_go_wanted_question"));
		if(!goModuleWantedByUser) {
            UIM.logErr(R.STR.getString("pow_go_user_refused_download"));
            return;
        }

        final String downloadUrl = R.URL.getString("go_module_download")+POW_FILE.getName();

        try {
            UIM.logInf("downloading " + POW_FILE.getName() + " from " + downloadUrl + " ...");
            URL website = new URL(downloadUrl);
            InputStream in = null;

            try {
                in = website.openStream();
            } catch (FileNotFoundException e) {
                UIM.logWrn(String.format(R.STR.getString("pow_go_not_available"), System.getProperty("os.name")+", "+System.getProperty("os.arch")));
                UIM.logErr(R.STR.getString("pow_go_download_failed"));
                return;
            }

            POW_FILE.createNewFile();
            Files.copy(in, POW_FILE.toPath(), (CopyOption)StandardCopyOption.REPLACE_EXISTING);
            UIM.logInf(R.STR.getString("pow_go_download_success"));
            setPermissions();
        } catch (IOException e) {
            UIM.logErr(R.STR.getString("pow_go_download_failed"));
            UIM.logException(e, false);
            POW_FILE.delete();
        }
    }

    /**
     * gives pow file permissions necessary to be executed on some OS
     * */
    private static void setPermissions() {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        try { Files.setPosixFilePermissions(POW_FILE.toPath(), perms); } catch(IOException | UnsupportedOperationException e) { }
    }
    
    public static void start(int threads) {

		if(!Configs.getBln(P.POW_USE_GO_MODULE)) {
			UIM.logWrn(R.STR.getString("pow_go_user_refused_use"));
			return;
		}
			
		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("os.arch").toLowerCase();
		final String fileExtension = os.substring(0, 3).equals("win") ? ".exe" : "";
		final String powFileName = "pow_"+os.substring(0, 3)+"_"+arch+fileExtension;

    	goPowAvailable = FileManager.exists(powFileName);
    	
    	if(!goPowAvailable) {
			UIM.logWrn(R.STR.getString("pow_go_file_missing"));
    		return;
    	}
    	
        Process proc = null;
        
		try {
			proc = Runtime.getRuntime().exec("./"+powFileName);
		} catch (IOException e) {
			UIM.logException(e, false);
		}
		
        InputStream in = proc.getInputStream();
        final OutputStream out = proc.getOutputStream();
		final Scanner s = new Scanner(in);
		scannerOpen.o = true;
		s.useDelimiter("\n");
        threads = Math.min(Math.max(1, threads), Runtime.getRuntime().availableProcessors());
        
        try {
			out.write((threads + "\n").getBytes());
			out.flush();
			
			String powName = s.hasNext() ? s.next().replace("\n", "") : "";
			if(powName.equals("PowGo"))
			    UIM.logWrn(R.STR.getString("pow_go_compilation_incomplete"));
			else
			    UIM.logInf(R.STR.getString("pow_go_optimal_method") + powName);
			
		} catch (IOException e) {
			UIM.logException(e, false);
		}
        
        powTrytes.o = "";
    	
    	final Thread t = new Thread() {
	        
    		public void run() {
    	        
    	        while(true) {
    	        	
    	        	synchronized (powTrytes) { try { powTrytes.wait(); } catch (InterruptedException e) { break; } }
    	        	
        	        try {
        				out.write((powTrytes.o+"\n").getBytes());
        				out.flush();
                        powTrytes.o = s.hasNext() ? s.next().replace("\n", "") : null;
                        powSuccess.o = true;
        			} catch (IOException | IllegalStateException e) {
        				UIM.logDbg(R.STR.getString("pow_go_communication_failed") + e.getMessage());
        			}

        	        synchronized (powTrytes) { powTrytes.notify(); }
    	        }
    		}
    	};
    	
    	t.start();
    	
    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		@Override
    		public void run() {
    			t.interrupt();
    			s.close();
    		}
    	});
    }

    private static String determinePowFileName() {
        final String os = System.getProperty("os.name").toLowerCase();
        final String arch = System.getProperty("os.arch").toLowerCase();
        final String fileExtension = os.substring(0, 3).equals("win") ? ".exe" : "";
        return "pow_"+os.substring(0, 3)+"_"+arch+fileExtension;
    }
    
    public static double getAvgPoWTime() {
		return amountPoW == 0 ? 0 : 0.001 * totalTimePoW / amountPoW;
	}
}