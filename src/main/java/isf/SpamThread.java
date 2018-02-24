package isf;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import isf.ui.UIManager;
import jota.model.Input;
import jota.model.Transfer;

public class SpamThread extends Thread {

	private static int totalTxs = 0;
	
	private static boolean paused = false;
	private static String tag = "IOTASPAM9DOT9COM99999999999";
	private static SpamThread spamThread;
	private static long timePauseStarted, totalPauses;
	
	private static final UIManager UIM = new UIManager("SpamThrd");
	
	@Override
	public void run() {
		
		TimeManager.addTask(new Task(120000, true) { @Override void onCall() { updateRemoteControl(); } });
		
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
	
	public static long getTotalPauses() {
		return totalPauses;
	}
	
	private static void updateRemoteControl() {
		JSONObject obj = APIManager.requestCommand();
		
		if(obj.getBoolean("pause") && !SpamThread.isPaused()) {
			timePauseStarted = System.currentTimeMillis();
			UIM.logWrn("spamming paused remotely by iotaspam.com: " + obj.getString("message"));
		} else if(!obj.getBoolean("pause") && SpamThread.isPaused()) {
			totalPauses += System.currentTimeMillis() - timePauseStarted;
			UIM.logWrn("spamming restarted remotely by iotaspam.com");
		}
		
		SpamThread.setPaused(obj.getBoolean("pause")); // TODO 1 -> boolean
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

	private static void setPaused(boolean paused) {
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