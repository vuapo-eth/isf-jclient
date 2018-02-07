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
		this.confirmedTxs = confirmedTxs;
	}

	public int getTotalTxs() {
		return totalTxs;
	}

	public void setTotalTxs(int totalTxs) {
		this.totalTxs = totalTxs;
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