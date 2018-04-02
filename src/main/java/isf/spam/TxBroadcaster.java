package isf.spam;

import isf.Main;
import isf.logic.TimeAbortCall;
import jota.dto.response.GetAttachToTangleResponse;

public class TxBroadcaster {

    private static int amountQueued = 0;
	
	public static void queueTrytes(final GetAttachToTangleResponse res) {
		
		if(res.getTrytes()[0] == null) return;
		
		final TimeAbortCall broadcastBomb = new TimeAbortCall("broadcasting tips", 10) {
			@Override
			public boolean onCall() {
				try {
					NodeManager.broadcastAndStore(res.getTrytes()[0]);
					return true;
				} catch (InterruptedException e) {
					return false;
				}
			}
		};
		
		new Thread(Main.SUPER_THREAD, "TxBroadcaster") {
			@Override
			public void run() {
                amountQueued++;
				while(!broadcastBomb.call(10));
				AddressManager.incrementSessionTxCount();
                amountQueued--;
			}
		}.start();
	}

    public static int getAmountQueued() {
        return amountQueued;
    }
}