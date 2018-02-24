package isf;

import java.util.ArrayList;
import java.util.List;

import jota.model.Input;
import jota.model.Transfer;

public class SpamThread extends Thread {

	private static int totalTxs = 0;
	
	private static boolean paused = false;
	private static String tag = "IOTASPAM9DOT9COM99999999999";
	private static SpamThread spamThread;
	
	@Override
	public void run() {
		
		spamThread = this;
		
		while(true) {
			
			if(paused) {
				synchronized (spamThread) {
					try {
						spamThread.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			sendTransfer();
			totalTxs++;
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
		
		if(!paused) {
			synchronized(spamThread) {
				spamThread.notify();
			}
		}
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