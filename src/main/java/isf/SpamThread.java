package isf;

import java.util.ArrayList;
import java.util.List;

import jota.model.Input;
import jota.model.Transfer;

public class SpamThread extends Thread {

	private static int totalTxs = 0;
	
	private static boolean paused = true;
	private static String tag = "IOTASPAM9DOT9COM99999999999";
	
	@Override
	public void run() {
		
		while(true) {
			sendTransfer();
			int totalTxsBackup = ++totalTxs;
			
			if(paused) {
				NodeManager.getUIM().logWrn("spamming thread paused remotely by ISF website");
				while(paused)
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				NodeManager.getUIM().logInf("spamming thread restarted remotely by ISF website");
			}
			
			if(totalTxsBackup % 10 == 0)
				AddressManager.updateTails();
				
			if(totalTxsBackup % (totalTxsBackup <= 200 ? 30 : 50) == 0)
				AddressManager.getTail().update();
			
			if(totalTxsBackup % 30 == 0)
				AddressManager.updateTails();
		}
	}
	
	private static void sendTransfer() {
		ArrayList<Transfer> transfers = new ArrayList<Transfer>();
		String message = UploadDataManager.getNextData();
		transfers.add(new Transfer(AddressManager.getSpamAddress(), 0, message, tag));
		List<Input> inputs = new ArrayList<Input>();
		NodeManager.sendTransfer(transfers, inputs);
	}
	
	protected static int getTotalTxs() {
		return totalTxs;
	}
	
	public static boolean isPaused() {
		return paused;
	}

	public static void setPaused(boolean paused) {
		SpamThread.paused = paused;
	}
	
	public static void setTag(String tag) {
		SpamThread.tag = trytesPadRight(tag, 27);
	}
	
	private static String trytesPadRight(String s, int n) {
		while (s.length() < n)
			s += '9';
		return s;
	}
}