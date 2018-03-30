package isf.spam;

import java.util.EmptyStackException;
import java.util.Stack;

import isf.logic.ObjectWrapper;
import isf.logic.TimeAbortCall;
import jota.dto.response.GetTransactionsToApproveResponse;

public class TipPool {

	private static Stack<GetTransactionsToApproveResponse> gttars = new Stack<GetTransactionsToApproveResponse>();
	private static int gttarsLimit = 5;
	
	static final ObjectWrapper REQUESTED_TIPS = new ObjectWrapper(0);
	static final ObjectWrapper REQUIRED_TIPS = new ObjectWrapper(gttarsLimit);
	
	public static void init() {
			
		for(int i = 0; i < NodeManager.getAmountOfAPIs(); i++) {
			final int api = i;
			
			new Thread() {
				@Override
				public void run() {
					
					TimeAbortCall tb = new TimeAbortCall("requesting transactions to approve (tips)", 10) {
						@Override
						public boolean onCall() {
							GetTransactionsToApproveResponse gttar = NodeManager.getTransactionsToApprove(api);
							if(gttar != null) {
								gttars.push(gttar);
								REQUIRED_TIPS.o = gttarsLimit-gttars.size();
							}
							return gttar != null;
						}

						@Override
						public void onNotToleratedFail(String failMsg) {
							NodeManager.connectToAnyNode(api, failMsg);
						}
					};
					
					while(true) {

					    do {
							synchronized (REQUIRED_TIPS) { try { REQUIRED_TIPS.wait(); } catch (InterruptedException e) { } }
						} while(!NodeManager.isAvailable(api));

						while((int)REQUESTED_TIPS.o < (int)REQUIRED_TIPS.o) {
							synchronized (REQUIRED_TIPS) { REQUESTED_TIPS.o = ((int)REQUESTED_TIPS.o)+1; }
							if(!tb.call(6));
							synchronized (REQUIRED_TIPS) { REQUESTED_TIPS.o = ((int)REQUESTED_TIPS.o)-1; }
						}
					}
				}
			}.start();
		}
	}
	
	public static GetTransactionsToApproveResponse getTransactionsToApprove() {
		gttarsLimit = Math.max((int)Math.ceil(SpamThread.getSpamSpeed()/5), 5);
		REQUIRED_TIPS.o = gttarsLimit-gttars.size();
		if((int)REQUIRED_TIPS.o > (int)REQUESTED_TIPS.o) synchronized (REQUIRED_TIPS) { REQUIRED_TIPS.notifyAll(); }
		try {
			return gttars.pop();
		} catch(EmptyStackException e) {
			return null;
		}
	}
	
	public static int gttarsQueueSize() {
		return gttars.size();
	}
	
	public static int gttarsLimit() {
		return gttarsLimit;
	}
}