package isf;

import isf.ui.UIManager;

public class Tail {
	private long timestamp;
	private int confirmedTxs, totalTxs;
	private final String trytes;
	private boolean lastCheckCompleted;

	public Tail(String trytes, long timestamp, int confirmedTxs, int totalTxs, boolean lastCheckCompleted) {
		this.trytes = trytes;
		this.timestamp = timestamp;
		this.confirmedTxs = confirmedTxs;
		this.totalTxs = totalTxs;
		this.lastCheckCompleted = lastCheckCompleted;
	}

	public Tail(String[] par) {
		this.trytes = par[0];
		this.timestamp = Long.parseLong(par[1]);
		this.confirmedTxs = Integer.parseInt(par[2]);
		this.totalTxs = Integer.parseInt(par[3]);
		this.lastCheckCompleted = Boolean.parseBoolean(par[4]);
	}
	
	public void update() {
		String[] hashes = NodeManager.findTransactionsByAddress(AddressManager.getAddressBase() + getTrytes());
		boolean[] states = NodeManager.getLatestInclusion(hashes);
		
		if(states.length == 0)
			return;
		
		int confirmedTxs = 0;
		for(boolean state : states)	if(state) confirmedTxs++;
		
		setTotalTxs(states.length);
		setConfirmedTxs(confirmedTxs);
	}
	
	public String toString() {
		return trytes
			+ "|" + UIManager.padLeft(timestamp+"", 10)
			+ "|" + UIManager.padLeft(confirmedTxs+"", 4)
			+ "|" + UIManager.padLeft(totalTxs+"", 4)
			+ "|" + lastCheckCompleted;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getConfirmedTxs() {
		return confirmedTxs;
	}

	public void setConfirmedTxs(int confirmedTxs) {
		this.confirmedTxs = Math.max(this.confirmedTxs, confirmedTxs);
	}

	public int getTotalTxs() {
		return totalTxs;
	}

	public void setTotalTxs(int totalTxs) {
		this.totalTxs = Math.max(this.totalTxs, totalTxs);
	}

	public String getTrytes() {
		return trytes;
	}

	public boolean isLastCheckCompleted() {
		return lastCheckCompleted;
	}
	
	public void setLastCheckCompleted(boolean lastCheckCompleted) {
		this.lastCheckCompleted = lastCheckCompleted;
	}
}