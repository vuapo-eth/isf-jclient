package isf;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;

import isf.ui.R;
import isf.ui.UIManager;

public class FileManager {

	private static final UIManager uim = new UIManager("FileMngr");
	
	public static boolean exists(String relativePath) {
		return new File(getJarPath() + relativePath).exists();
	}
	
	public static void mkdirs(String relativePath) {
		new File(getJarPath() + relativePath).mkdirs();
	}
	
	public static File getFile(String relativePath) {
		return new File(getJarPath() + relativePath);
	}
	
	public static String read(String relativePath) {
		
		try {
			FileInputStream fstream = new FileInputStream(getJarPath() + relativePath);
			DataInputStream in = new DataInputStream(fstream);
			return readFile(in);
		} catch (Exception e){
			uim.logException(e, true);
			return "";
		}
	}
	
	private static String readFile(DataInputStream in) {
		String strLine = "", str = "";
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null)
				str += (strLine.length() > 0? "\n" : "") + strLine;
			in.close();
		} catch (Exception e){
			uim.logException(e, true);
		}
		
		return str;
	}
	
	public static String getJarPath() {
		try {
			String[] dirs = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().split("/");
			String path = dirs[0] + "/";
			for (int i = 1; i < dirs.length-1; i++)
				path += dirs[i]+"/";
			return path;
		} catch (URISyntaxException e) {
			uim.logException(e, true);
			return "./";
		}
	}
	
	public static void write(String path, String data) {
		write(path, data.getBytes());
	}
		
	private static void write(String path, byte[] data) {
		//uim.logDbg("writing into file '"+path+"'");
	    File targetFile = getFile(path);
	    
		try {
		    if(!targetFile.exists())
		    	targetFile.createNewFile();
		    
		    OutputStream out = new FileOutputStream(targetFile);
			out.write(data);
			out.close();
		} catch (IOException e) {
			uim.logWrn(String.format(R.STR.getString("file_writing_failed"), targetFile.getAbsolutePath()));
			uim.logException(e, true);
		}
	}
}