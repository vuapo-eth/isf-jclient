package isf;

import jota.dto.response.GetAttachToTangleResponse;

public class TxBroadcaster {
	
	public static void queueTrytes(GetAttachToTangleResponse res) {
		final TimeBomb broadcastBomb = new TimeBomb("broadcasting tips", 10) {
			@Override
			void onCall() {
				NodeManager.broadcastAndStore(res.getTrytes());
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