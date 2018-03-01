package isf;

import isf.ui.UIManager;

public abstract class TimeBomb {
	private static final UIManager UIM = new UIManager("TimeBomb");

	private final String actionName;
	private final int tolerance;
	
	private int fails;
	
	public TimeBomb(String actionName, int tolerance) {
		this.actionName = actionName;
		this.tolerance = tolerance;
	}
	
	public boolean call(int timeLimitSeconds) {
		
		final Result r = new Result();
		
		Thread t = new Thread() {
			@Override
			public void run() {
				r.setResult(onCall());
				synchronized (r) { r.notify(); }
			}
		};
		t.start();
		
		try {
			synchronized (r) { r.wait(timeLimitSeconds*1000); }
		} catch (InterruptedException e) {
			UIM.logException(e, true);
		}
		if(!r.getResult()) {
			fails++;
			t.interrupt();
		}
		
		if(tolerance > 0 && fails >= tolerance) {
			UIM.logWrn("action '" + actionName + "' took too long and was aborted after "+timeLimitSeconds+" seconds" + (tolerance > 1 ? " (this message only shows up on every "+tolerance+"th abortion)" : ""));
			fails = 0;
		}
		
		return r.getResult();
	}
	
	abstract boolean onCall();
}

class Result {
	private boolean result = false;
	
	boolean getResult() {
		return result;
	}
	
	void setResult(boolean result) {
		this.result = result;
	}
}