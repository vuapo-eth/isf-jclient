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
import isf.ObjectCarrier;
import isf.P;
import isf.ui.UIManager;
import jota.IotaLocalPoW;
import jota.utils.Converter;


public class GOldDiggerLocalPoW implements IotaLocalPoW {

	private static int amountPoW = 0;
	private static long totalTimePoW = 0;
	private static final ObjectCarrier oc = new ObjectCarrier(null);
	private static final ObjectCarrier success = new ObjectCarrier(false);
	private static final ObjectCarrier scannerOpen = new ObjectCarrier(false);
	private static boolean goPowAvailable;
	private static final UIManager UIM = new UIManager("GldDiggr");
    PearlDiver pearlDiver = new PearlDiver();

    public static void download() {

		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("os.arch").toLowerCase();
		final String fileExtension = os.substring(0, 3).equals("win") ? ".exe" : "";
		final String powFileName = "pow_"+os.substring(0, 3)+"_"+arch+fileExtension;
    	File targetFile = new File(powFileName);
    	
    	if(targetFile.exists()) return;
		
		Set<PosixFilePermission> perms = new HashSet<>();
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_WRITE);
		perms.add(PosixFilePermission.OWNER_EXECUTE);
		
		if(UIM.askForBoolean("do you want to download an optimized GO proof-of-work module and increase your spam performance by approx. 50-100%?")) {
			String downloadUrl = "https://github.com/mikrohash/isf-jclient/releases/download/v1.0.9/"+powFileName;
			
    	    try {
    			UIM.logInf("downloading " + powFileName + " from " + downloadUrl);
    			URL website = new URL(downloadUrl);
    			InputStream in = null;
    			
    			try {
    				in = website.openStream();
    			} catch (FileNotFoundException e) {
    				UIM.logWrn("Unfortunately, the GO pow module for your system '"+os+"-"+arch+"' is not available. Please contact us via contact@iotaspam.com, so we can add it to our collection. If you want to compile it yourself, you will find instructions README.md");
    				UIM.logWrn("could not download proof-of-work module, will use low-performing java proof-of-work module instead");
        			return;
    			}

    	    	targetFile.createNewFile();
    			Files.copy(in, targetFile.toPath(), (CopyOption)StandardCopyOption.REPLACE_EXISTING);
    			UIM.logInf("download complete");
				Files.setPosixFilePermissions(targetFile.toPath(), perms);
    		} catch (IOException e) {
    			UIM.logErr("could not download proof-of-work module, will use low-performing java proof-of-work module instead");
    			UIM.logException(e, false);
    			targetFile.delete();
    		}
		} else {
			UIM.logWrn("alright, since you don't want to optimize your spam, we will use the low-performing java proof-of-work module instead");
		}
    }
    
    public static void start(int threads) {

		if(!Configs.getBln(P.POW_USE_GO_MODULE)) {
			UIM.logWrn("since you don't want to use the optimized GO pow module, we will use the low-performing java pow module instead");
			return;
		}
			
		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("os.arch").toLowerCase();
		final String fileExtension = os.equals("win") ? ".exe" : "";
		final String powFileName = "pow_"+os.substring(0, 3)+"_"+arch+fileExtension;
		
    	goPowAvailable = FileManager.exists(powFileName);
    	
    	if(!goPowAvailable) {
			UIM.logWrn("could not find the optimized GO pow module, will use the low-performing java pow module instead");
    		return;
    	}
    	
        Process proc = null;
        
		try {
	    	//File targetFile = new File(powFileName);
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
			UIM.logInf("Using optimal proof-of-work method for this machine: " + powName);
			
		} catch (IOException e) {
			UIM.logException(e, false);
		}
        
        oc.o = "";
    	
    	final Thread t = new Thread() {
	        
    		public void run() {
    	        
    	        while(true) {
    	        	
    	        	synchronized (oc) { try { oc.wait(); } catch (InterruptedException e) { break; } }
    	        	
        	        try {
        				out.write((oc.o+"\n").getBytes());
        				out.flush();
        			} catch (IOException e) {
        				UIM.logDbg("Failed to communicate with pow file: " + e.getMessage());
        			}
        	        
        	        try {
            	        oc.o = s.hasNext() ? s.next().replace("\n", "") : null;
            	        success.o = true;
        	        } catch (IllegalStateException e) {
        				UIM.logDbg("Failed to communicate with pow file: " + e.getMessage());
        	        }
        	        synchronized (oc) { oc.notify(); }
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

    private String javaPow(String trytes, int minWeightMagnitude) {
        int[] trits = Converter.trits(trytes);
        if (!pearlDiver.search(trits, minWeightMagnitude, Configs.getInt(P.POW_CORES)))
            throw new IllegalStateException("PoW aborted: took too long");
        return Converter.trytes(trits);
    }
    
    public String performPoW(String trytes, int minWeightMagnitude) {
        long timeStarted = System.currentTimeMillis();
        String nonced = goPowAvailable ? goPow(trytes) : javaPow(trytes, minWeightMagnitude);
        totalTimePoW += System.currentTimeMillis() - timeStarted;
        amountPoW++;
    	return nonced;
    }
    
    public static String goPow(String trytes) {
    	oc.o = trytes;
    	success.o = false;
    	
    	synchronized (oc) {
    		oc.notify();
    		
			try { oc.wait(); } catch (InterruptedException e) { }
		}
    	return (boolean)success.o ? (String)oc.o : null;
    }
    
    public static double getAvgPoWTime() {
		return amountPoW == 0 ? 0 : 0.001 * totalTimePoW / amountPoW;
	}
}