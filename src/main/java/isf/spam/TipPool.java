package isf.spam;

import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import isf.Main;
import isf.logic.ObjectWrapper;
import isf.logic.TimeAbortCall;
import isf.ui.R;
import isf.ui.UIManager;
import jota.dto.response.GetTransactionsToApproveResponse;

public class TipPool {

	public static final ThreadGroup TIP_POOL_THREAD_GROUP = new ThreadGroup("TipPoolThread");
    private static final UIManager UIM = new UIManager("TipPool");

	private static Stack<String[]> gttars = new Stack<String[]>();
	private static int gttarsLimit = 5;
	private static long lastTimeEmpty = 0;

    private static final ObjectWrapper REQUESTED_TIPS = new ObjectWrapper(0);
    private static final ObjectWrapper REQUIRED_TIPS = new ObjectWrapper(gttarsLimit);
	
	public static void init() {
			
		for(int i = 0; i < NodeManager.getAmountOfAPIs(); i++) {
			final int api = i;
			
			new Thread(TIP_POOL_THREAD_GROUP, "TipPool-"+api) {
				@Override
				public void run() {
					
					TimeAbortCall tb = new TimeAbortCall(R.STR.getString("action_request_tips"), 10) {
						@Override
						public boolean onCall() {
                            final GetTransactionsToApproveResponse gttar = NodeManager.getTransactionsToApprove(api);
							if(gttar != null) {
                                final String[] gttarString = {gttar.getBranchTransaction(), gttar.getTrunkTransaction()};
                                gttars.push(gttarString);

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
	
	public static String[] getTransactionsToApprove() {
		gttarsLimit = Math.max((int)Math.ceil(SpamThread.getSpamSpeed()/5), 5);
		REQUIRED_TIPS.o = gttarsLimit-gttars.size();
		if((int)REQUIRED_TIPS.o > (int)REQUESTED_TIPS.o) synchronized (REQUIRED_TIPS) { REQUIRED_TIPS.notifyAll(); }
		try {
			return gttars.pop();
		} catch(EmptyStackException e) {
		    if(lastTimeEmpty < System.currentTimeMillis() - 60000) {
                if(lastTimeEmpty > 0)
                    UIM.logWrn(R.STR.getString("tip_pool_empty"));
                lastTimeEmpty = System.currentTimeMillis();
            }

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