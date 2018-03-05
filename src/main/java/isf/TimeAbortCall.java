package isf;

import isf.ui.UIManager;

public abstract class TimeAbortCall {
	private static final UIManager UIM = new UIManager("TimeBomb");

	private final String actionName;
	private final int tolerance;
	
	private int fails;
	
	public TimeAbortCall(String actionName, int tolerance) {
		this.actionName = actionName;
		this.tolerance = tolerance;
	}
	
	public boolean call(int timeLimitSeconds) {

		final ObjectCarrier res = new ObjectCarrier(false);
		final ObjectCarrier success = new ObjectCarrier(false);
		
		Thread t = new Thread() {
			@Override
			public void run() {
				res.o = onCall();
				success.o = true;
				synchronized (res) { res.notify(); }
			}
		};
		t.start();
		
		try {
			synchronized (res) { res.wait(timeLimitSeconds*1000); }
		} catch (InterruptedException e) {
			UIM.logException(e, true);
		}
		if(!(boolean)success.o) {
			fails++;
			t.interrupt();
		}
		
		if(tolerance > 0 && fails >= tolerance) {
			String failMsg = "action '" + actionName + "' took too long and was aborted after "+timeLimitSeconds+" seconds" + (tolerance > 1 ? " (this message only shows up on every "+tolerance+"th abortion)" : "");
			onNotToleratedFail(failMsg);
			UIM.logWrn(failMsg);
			fails = 0;
		}
		
		return (boolean)res.o;
	}
	
	public void onNotToleratedFail(String failMsg) { }
	
	public abstract boolean onCall();
}