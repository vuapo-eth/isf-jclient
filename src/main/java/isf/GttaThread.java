package isf;

import java.util.LinkedList;
import java.util.Queue;

import jota.dto.response.GetTransactionsToApproveResponse;

public class GttaThread extends Thread {
	
	private static Queue<GetTransactionsToApproveResponse> gttars = new LinkedList<GetTransactionsToApproveResponse>();
	private static int gttarsLimit;
	
	@Override
	public void run() {
		gttarsLimit = Configs.getInt(P.THREADS_GTTARS_SIZE);
		while(true) {
			if(gttars.size() < gttarsLimit)
				gttars.add(NodeManager.getTransactionsToApprove());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static GetTransactionsToApproveResponse getTransactionsToApprove() {
		return gttars.poll();
	}
	
	public static int gttarsQueueSize() {
		return gttars.size();
	}
	
	public static int gttarsLimit() {
		return gttarsLimit;
	}
}