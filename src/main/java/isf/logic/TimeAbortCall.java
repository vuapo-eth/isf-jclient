package isf.logic;

import isf.Logger;
import isf.ui.R;
import isf.ui.UIManager;

public abstract class TimeAbortCall {
    private static final UIManager UIM = new UIManager("TimeAbrt");
    public static final ThreadGroup TIME_ABORT_CALL_THREAD = new ThreadGroup("TimeAbortCallThread");
    public static final ThreadGroup TA_NODE_CONNECT = new ThreadGroup("TA_NODE_CONNECT");
    public static final ThreadGroup NODE_INFO = new ThreadGroup("NODE_INFO");
    public static final ThreadGroup TIPS = new ThreadGroup("TIPS");
    public static final ThreadGroup TA_SPAM = new ThreadGroup("TA_SPAM");
    public static final ThreadGroup TA_BROADCAST = new ThreadGroup("TA_BROADCAST");

	private final String actionName;
	private final int tolerance;

    private static long threadIdCounter = 0;
    private ThreadGroup tg = null;

	private int fails;
	
	public TimeAbortCall(String actionName, int tolerance, ThreadGroup tg) {
		this.actionName = actionName;
		this.tolerance = tolerance;
		this.tg = tg;
	}
	
	public boolean call(int timeLimitSeconds) {

		final ObjectWrapper res = new ObjectWrapper(false);
		final ObjectWrapper success = new ObjectWrapper(false);

		Thread t = new Thread(tg, "TimeAbortCall-"+(threadIdCounter++)) {
			@Override
			public void run() {
				res.o = onCall();
				success.o = true;
				synchronized (res) { res.notify(); }
			}
        };

		while(true) {
            try {
                t.start();
                break;
            } catch (OutOfMemoryError e) {
                UIM.logErr(R.STR.getString("thread_out_of_memory"));
                UIM.logDbg("Heap: " + Logger.buildHeapString());
                UIM.logDbg("Threads: " + Logger.buildThreadString());
                UIM.logException(e, false);
                try { Thread.sleep(10000); } catch (InterruptedException ie) { return false; }
            }
        }
		
		try {
			synchronized (res) { res.wait(timeLimitSeconds*1000); }
		} catch (InterruptedException e) {
			t.interrupt();
			UIM.logException(e, true);
		}
		if(!(boolean)success.o) {
			fails++;
			t.interrupt();
		}
		
		if(tolerance > 0 && fails >= tolerance) {

			String failMsg = String.format(R.STR.getString("abort_message"), actionName, timeLimitSeconds) + (tolerance > 1 ? String.format(" ("+R.STR.getString("abort_tolerance")+")", tolerance) : "");
			onNotToleratedFail(failMsg);
			UIM.logWrn(failMsg);
			fails = 0;
		}
		
		return (boolean)res.o;
	}
	
	public void onNotToleratedFail(String failMsg) { }
	
	public abstract boolean onCall();
}