package iota;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cfb.pearldiver.PearlDiverLocalPoW;
import iota.ui.UIManager;
import jota.dto.response.FindTransactionResponse;
import jota.dto.response.GetInclusionStateResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.dto.response.SendTransferResponse;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;

public class NodeManager {

	private static final UIManager uim_static = new UIManager("NodeMngr");
	private final UIManager uim;
	
	private static final int DEPTH = 4, MIN_WEIGHT_MAGNITUDE = 14, INCONSISTENT_TIPS_PAIR_TOLERANCE = 60;
	
	private static ArrayList<String> nodeList = new ArrayList<String>();
	
	private ExtIotaAPI api = null;
	
	public NodeManager(int spamThreadId) {
		uim = new UIManager("NdMngr-"+spamThreadId);
		connectToNode(null);
	}
	
	public void connectToNode(String node) {
		
		String nodeSyncedMsg = null;
		boolean nodeSynced = node != null;
		
		do {
				
			if(node != null) {
				if(api == null)
					uim.logDbg("opening connection to node '"+node+"'");
				else if(nodeSyncedMsg != null) //if (!buildNodeAddress().equals(node))
					uim.logDbg("node '" + buildNodeAddress() + "' is not synced ("+nodeSyncedMsg+"), changing node to '"+node+"'");
				
				String protocol = node.split(":")[0];
				String host = node.split(":")[1].replace("//", "");
				String port = node.split(":")[2];

				try {
					api = (ExtIotaAPI) new ExtIotaAPI.Builder()
					        .protocol(protocol)
					        .host(host)
					        .port(port)
					        .localPoW(new PearlDiverLocalPoW())
					        .build();
				} catch (IllegalArgumentException e) {
					uim.logException(e, true);
				}
		
				sleep(3000);
				nodeSyncedMsg = isSolidSubtangleUpdated();
				nodeSynced = nodeSyncedMsg == null;
			}
			
			if(!nodeSynced) {
				if(nodeList.size() == 0)
					try {
						throw new Exception("could not connect to node: node list empty");
					} catch (Exception e) {
						uim.logException(e, true);
					}
				else if(nodeList.size() == 1) {
					if(buildNodeAddress().equals(nodeList.get(0))) {
						uim.logWrn("spammer paused for 60 seconds ("+nodeSyncedMsg+"): waiting for node to get back in sync" + node);
						sleep(60000);
					}
					node = nodeList.get(0);
				} else {
					if(nodeList.get(0).equals(buildNodeAddress())) {
						nodeList.add(nodeList.get(0));
						nodeList.remove(0);
					}
					node = nodeList.get(0);

					nodeList.remove(0);
					nodeList.add(node);
				}
			}
		} while(!nodeSynced);
	}
	
	public String[] findTransactionsByAddress(String address) {
		String[] addresses = {address};
		FindTransactionResponse findTransactionResponse = null;
		
		while(findTransactionResponse == null) {
			try {
				 findTransactionResponse = api.findTransactionsByAddresses(addresses);
			} catch (Throwable e) {
				handleThrowableFromIotaAPI("could not check state of spam address '"+address+"'", e);
			}
		}
		
		return findTransactionResponse.getHashes();
	}
	
	public boolean[] getInclusionStates(String[] hashes, String tip) {

		String[] tips = {tip};
		GetInclusionStateResponse getInclusionStateResponse = null;
		
		while(getInclusionStateResponse == null) {
			try {
				getInclusionStateResponse = api.getInclusionStates(hashes, tips);
			} catch (Throwable e) {
				handleThrowableFromIotaAPI("could not check latest inclusion states", e);
			}
		}
		return getInclusionStateResponse.getStates();
	}
	
	public String getLatestMilestone() {
		GetNodeInfoResponse getNodeInfoResponse = null;
		do {
			if(getNodeInfoResponse != null) {
				uim.logDbg("latest milestone of node older than 10 minutes: '" + buildNodeAddress() + "'");
				connectToNode(null);
				sleep(5000);
			}
			getNodeInfoResponse = null;
			while(getNodeInfoResponse == null) {
				try {
					getNodeInfoResponse = api.getNodeInfo();
				} catch (Throwable e) {
					handleThrowableFromIotaAPI("could not receive getNodeInfo", e);
				}
			}
		} while(findTractionByHash(getNodeInfoResponse.getLatestMilestone()).getTimestamp() < System.currentTimeMillis()/1000-600);
		return getNodeInfoResponse.getLatestMilestone();
	}
	
	public String isSolidSubtangleUpdated() {
		GetNodeInfoResponse getNodeInfoResponse = null;
		try {
			getNodeInfoResponse = api.getNodeInfo();
		} catch (Throwable e) {
			return e.getMessage() == null || e.getMessage() == "" ? "not sure why though" : e.getMessage();
		}
		
		if(getNodeInfoResponse == null)
			return "did not receive response";
		
		if(Math.abs(getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex()-getNodeInfoResponse.getLatestMilestoneIndex()) > 3) {
			return "solid subtangle is not updated: lacking "+(getNodeInfoResponse.getLatestMilestoneIndex()-getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex())+" milestones behind";
		}
		
		long secondsBehind = System.currentTimeMillis()/1000-findTractionByHash(getNodeInfoResponse.getLatestSolidSubtangleMilestone()).getTimestamp();
		if(secondsBehind > 600) {
			return "lacking "+(secondsBehind/60)+" minutes behind";
		}
		
		return null;
	}
	
	public Transaction findTractionByHash(String hash) {
		String[] hashes = { hash };
		List<Transaction> transactions = null;

		while(transactions == null) {
			try {
				transactions = api.getTransactionsObjects(hashes);
			} catch (Throwable e) {
				handleThrowableFromIotaAPI("could not find transactions", e);
			}
		}
		return transactions.get(0);
	}
	
	public String getTransactionsToApprove() {
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;
		
		while(getTransactionsToApproveResponse == null) {
			try {
				getTransactionsToApproveResponse = api.getTransactionsToApprove(DEPTH);
			} catch (Throwable e) {
				handleThrowableFromIotaAPI("could not get transactions to approve", e);
			}
		}
		return getTransactionsToApproveResponse.getBranchTransaction() + "|" + getTransactionsToApproveResponse.getTrunkTransaction();
	}
	
	public void sendTransfer(List<Transfer> transfers, Input[] inputs) {
		
		SendTransferResponse sendTransferResponse = null;
		int inconsistentTipsPair = 0;
		
		while(sendTransferResponse == null) {
			try {
				sendTransferResponse = api.sendTransfer("", 2, DEPTH, MIN_WEIGHT_MAGNITUDE, transfers, inputs, AddressManager.getSpamAddress());
			} catch (Throwable e) {
				if(e.getMessage() != null && e.getMessage().contains("inconsistent tips pair selected")) {
					if(++inconsistentTipsPair == INCONSISTENT_TIPS_PAIR_TOLERANCE) {
						inconsistentTipsPair = 0;
						handleThrowableFromIotaAPI("could not send transfer", new Exception("node '"+buildNodeAddress()+"' selected an inconsistent tips pair "+INCONSISTENT_TIPS_PAIR_TOLERANCE+" times"));
					}
					sleep(1000);
				} else
					handleThrowableFromIotaAPI("could not send transfer", e);
				sendTransferResponse = null;
			}
		}
	}
	
	private void handleThrowableFromIotaAPI(String failedAction, Throwable e) {
		
		if(IllegalAccessError.class.isAssignableFrom(e.getClass()))
			uim.logDbg(failedAction + ": '" +
				(e.getMessage().contains("\"error\"") ? (e.getMessage().split("\"error\":\"")[1].split("\"")[0] + "'") : e.getMessage()));
		else if (IllegalStateException.class.isAssignableFrom(e.getClass()) && e.getMessage().contains("Failed to connect to"))
			uim.logDbg(e.getMessage());
		else if(e.getMessage() == null)
			uim.logDbg(failedAction + " (not sure why though)");
		else
			uim.logException(e, false);
		
		connectToNode(null);
	}
	
	public boolean isNodeSynced() {
		
		uim.logDbg("checking if node is synced: '"+ buildNodeAddress() +"'");
		
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;
		
		try {
			getTransactionsToApproveResponse = api.getTransactionsToApprove(DEPTH);
		}  catch (Throwable e) {
			return false;
		}
		if(getTransactionsToApproveResponse == null) return false; // it happens, for whatever reason?!
		String[] hashes = {getTransactionsToApproveResponse.getBranchTransaction(), getTransactionsToApproveResponse.getTrunkTransaction()};
		List<Transaction> transactions = api.getTransactionsObjects(hashes);
		if(transactions.size() == 0)
			return false;
		long newerTimestamp = Math.max(transactions.get(0).getAttachmentTimestamp(), transactions.get(1).getAttachmentTimestamp());

		return newerTimestamp > System.currentTimeMillis() /1000-5*60;
	}
	
	public static String getNodeListString() {
		String nodeListString = "";
		for(int i = 0; i < nodeList.size(); i++) {
			String node = nodeList.get(i);
			nodeListString += node + (i < nodeList.size()-1 ? ", " : "");
		}
		return nodeListString;
	}
	
	public static void addNode(String address) {
		address = address.toLowerCase().replaceAll("/$", "");
		if(address.matches("^(http|https)://[a-z0-9\\\\.\\\\-]{6,}:[0-9]{1,5}$")) {
			uim_static.logDbg("adding node to node list: '"+address+"'");
			nodeList.add(address);
		} else {
			uim_static.logWrn("address is not correct: '" + address + "'");
		}
		
	}
	
	public static void importRemoteNodeList() {
		String[] nodes = SpamFundAPI.requestNodes();
		for(String node : nodes)
			addNode(node);
	}
	
	public static void addToNodeList(String nodeListString) {
		String[] nodes = (nodeListString.length() == 0) ? new String[0] : nodeListString.split("(,)");
		for(String node : nodes)
			addNode(node);
	}
	
	public static void shuffleNodeList() {
		Collections.shuffle(nodeList);
	}
	
	public void reconnect() {
		connectToNode(buildNodeAddress());
	}
	
	private String buildNodeAddress() {
		return (api == null ? "" : api.getProtocol() + "://" + api.getHost() + ":" + api.getPort());
	}
	
	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			uim.logException(e, true);
		}
	}
	
	public UIManager getUIM() {
		return uim;
	}
}
