package isf.spam;

import isf.APIManager;
import isf.Configs;
import isf.Main;
import isf.P;
import isf.logic.CronJob;
import isf.logic.CronJobManager;
import isf.logic.TimeAbortCall;
import org.json.JSONObject;

import iota.GOldDiggerLocalPoW;
import isf.ui.UIManager;

public class SpamThread extends Thread {
	
	private static boolean paused = false;
	private static String tag = "IOTASPAM9DOT9COM99999999999";
	private static SpamThread spamThread;
	private static long timePauseStarted, totalPauses;
	private static long timeStarted;
	
	private static final UIManager UIM = new UIManager("SpamThrd");
	
	private static final TimeAbortCall SPAM_BOMB = new TimeAbortCall("sending spam transaction", 1) {
		@Override
		public boolean onCall() {
			return NodeManager.sendSpam();
		}
	};

    public SpamThread() {
        super("SpamThread");
    }

    public void init() {
        spamThread = this;

        GOldDiggerLocalPoW.start(Configs.getInt(P.POW_CORES));

        if(Main.isInOnlineMode()) {
            updateRemoteControl();
            CronJobManager.addCronJob(new CronJob(120000, false, false) { @Override public void onCall() { updateRemoteControl(); } });
        }

        start();
    }

    @Override
	public void run() {

        timeStarted = System.currentTimeMillis();
		
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
			
			SPAM_BOMB.call(Configs.getInt(P.POW_ABORT_TIME));
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
		
		SpamThread.setPaused(obj.getBoolean("pause"));
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
	
	public static String getTag() {
		return tag;
	}
	
	public static void setTag(String tag) {
		SpamThread.tag = trytesPadRight(tag, 27);
	}
	
	private static String trytesPadRight(String s, int n) {
		while (s.length() < n)
			s += '9';
		return s;
	}
	
	public static int getTotalSpam() {
		return AddressManager.getSessionTxCount() + AddressManager.getPreSessionTransactions();
	}
	
	public static double getSpamSpeed() {
		int timeRunning = getTimeRunning();
		return timeRunning > 0 ? 60000.0*AddressManager.getSessionTxCount()/timeRunning : 0;
	}
	
	public static int getTimeRunning() {
		return timeStarted == 0 ? 0 : (int) (System.currentTimeMillis() - timeStarted - getTotalPauses());
	}
}