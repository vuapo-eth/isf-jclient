package isf;

import jota.dto.response.GetAttachToTangleResponse;

public class TxBroadcaster {
	
	public static void queueTrytes(final GetAttachToTangleResponse res) {
		
		final TimeBomb broadcastBomb = new TimeBomb("broadcasting tips", 10) {
			@Override
			boolean onCall() {
				NodeManager.broadcastAndStore(res.getTrytes());
				return true;
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