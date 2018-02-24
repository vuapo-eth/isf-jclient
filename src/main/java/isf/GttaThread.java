package isf;

import java.util.EmptyStackException;
import java.util.Stack;

import jota.dto.response.GetTransactionsToApproveResponse;

public class GttaThread extends Thread {
	
	private static Stack<GetTransactionsToApproveResponse> gttars = new Stack<GetTransactionsToApproveResponse>();
	private static int gttarsLimit;
	
	@Override
	public void run() {
		gttarsLimit = Configs.getInt(P.THREADS_GTTARS_SIZE);
		while(true) {
			if(gttars.size() < gttarsLimit)
				gttars.push(NodeManager.getTransactionsToApprove());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static GetTransactionsToApproveResponse getTransactionsToApprove() {
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