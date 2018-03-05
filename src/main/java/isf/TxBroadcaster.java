package isf;

import jota.dto.response.GetAttachToTangleResponse;

public class TxBroadcaster {
	
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
		
		new Thread() {
			@Override
			public void run() {
				while(!broadcastBomb.call(10));
				AddressManager.incrementSessionTxCount();
			}
		}.start();
	}
}