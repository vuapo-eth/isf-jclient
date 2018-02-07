package iota;

import iota.ui.UIManager;

public class Tail {
	private int timestamp, confirmedTxs, totalTxs;
	private final String trytes;
	private String milestone;
	
	public Tail(String line) {
		String[] par = line.split("\\|");
		trytes = par[0];
		timestamp = Integer.parseInt(par[1]);
		confirmedTxs = Integer.parseInt(par[2]);
		totalTxs = Integer.parseInt(par[3]);
		milestone = par[4];
	}
	
	public void update(NodeManager nodeManager) {
		String[] hashes = nodeManager.findTransactionsByAddress(AddressManager.ADDRESS_BASE + getTrytes());
		String latestMilestone = nodeManager.getLatestMilestone();
		boolean[] states = nodeManager.getInclusionStates(hashes, latestMilestone);
		
		if(states.length == 0)
			return;
		
		int confirmedTxs = 0;
		for(boolean state : states)	if(state) confirmedTxs++;
		
		setTotalTxs(states.length);
		setMilestone(latestMilestone);
		setConfirmedTxs(confirmedTxs);
	}
	
	public String toString() {
		return trytes + "|" + UIManager.padLeft(timestamp+"", 10)
		+ "|" + UIManager.padLeft(confirmedTxs+"", 4)
		+ "|" + UIManager.padLeft(totalTxs+"", 4)
		+ "|" + milestone;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(int timestamp) {
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

	public String getMilestone() {
		return milestone;
	}

	public void setMilestone(String milestone) {
		this.milestone = milestone;
	}
}