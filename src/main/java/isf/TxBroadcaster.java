package isf;

import java.util.LinkedList;
import java.util.Queue;

import jota.dto.response.GetAttachToTangleResponse;

public class TxBroadcaster extends Thread {
	
	private static Queue<GetAttachToTangleResponse> trytesQueue = new LinkedList<GetAttachToTangleResponse>();
	
	@Override
	public void run() {
		while(true) {
			GetAttachToTangleResponse trytes = trytesQueue.poll();
			if(trytes != null) {
				NodeManager.broadcastAndStore(trytes.getTrytes());
			}
			
			if(trytesQueue.size() == 0)
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static void queueTrytes(GetAttachToTangleResponse res) {
		trytesQueue.add(res);
	}
}