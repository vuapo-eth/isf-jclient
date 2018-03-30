package isf.logic;

import isf.ui.R;
import isf.ui.UIManager;

public abstract class TimeAbortCall {
	private static final UIManager UIM = new UIManager("TimeAbrt");

	private final String actionName;
	private final int tolerance;
	
	private int fails;
	
	public TimeAbortCall(String actionName, int tolerance) {
		this.actionName = actionName;
		this.tolerance = tolerance;
	}
	
	public boolean call(int timeLimitSeconds) {

		final ObjectWrapper res = new ObjectWrapper(false);
		final ObjectWrapper success = new ObjectWrapper(false);
		
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