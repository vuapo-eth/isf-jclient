package isf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import isf.ui.UIManager;
import iota.GoldDiggerLocalPoW;
import iota.IotaAPI;
import jota.dto.response.FindTransactionResponse;
import jota.dto.response.GetInclusionStateResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.dto.response.SendTransferResponse;
import jota.dto.response.StoreTransactionsResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;

public class NodeManager {

	private static final UIManager uim = new UIManager("NodeMngr");
	
	private static final int DEPTH = 4, MIN_WEIGHT_MAGNITUDE = 14, INCONSISTENT_TIPS_PAIR_TOLERANCE = 20;
	
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.-]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	
	private static ArrayList<String> nodeList = new ArrayList<String>();
	
	private static int amountGetTxsToApprove = 0;
	private static long totalTimeGetTxsToApprove = 0;
	private static IotaAPI[] apis;
	private static int[] inconsistentTipsPairs;
	private static long[] lastSyncCheck;
	private static int apiIndex = 0;
	private static int availableAPIs = 0;
	
	public static int getAmountOfAvailableAPIs() {
		return availableAPIs;
	}
	
	public static int getAmountOfAPIs() {
		return apis.length;
	}
	
	public static int getAmountOfNodes() {
		return nodeList.size();
	}
	
	public static void clearNodes() {
		nodeList = new ArrayList<String>();
	}
	
	public static void init() {
		shuffleNodeList();
		uim.logDbg("node list includes " + nodeList.size() + " nodes");
		apis = new IotaAPI[Math.min(Configs.getInt(P.NODES_AMOUNT_ROTATION), nodeList.size())];
		lastSyncCheck = new long[apis.length];
		inconsistentTipsPairs = new int[apis.length];
		for(int i = 0; i < apis.length; i++) {
			final int iFinal = i;
			new Thread() {
				public void run() {
					connectToNode(null, iFinal, null);
					lastSyncCheck[iFinal] = System.currentTimeMillis()/1000;
					if(availableAPIs == 0) apiIndex = iFinal;
					availableAPIs++;
				};
			}.start();
		}
	}
	
	public static void connectToNode(final String parNode, final int api, final String parNodeSyncedMsg) {
		availableAPIs--;
		
		new Thread() {
			@Override
			public void run() {
				
				String nodeSyncedMsg = parNodeSyncedMsg, node = parNode;
				boolean nodeSynced = node != null;
				
				do {
					if(node != null) {
						if(nodeSyncedMsg != null)
							uim.logDbg("node '" + buildNodeAddress(api) + "' ["+api+"] is not synced ("+nodeSyncedMsg+"), changing node to '"+node+"'");
						
						String protocol = node.split(":")[0];
						String host = node.split(":")[1].replace("//", "");
						String port = node.split(":")[2];

						try {
							apis[api] = (IotaAPI) new IotaAPI.Builder()
							        .protocol(protocol)
							        .host(host)
							        .port(port)
							        .localPoW(new GoldDiggerLocalPoW())
							        .build();
						} catch (IllegalArgumentException e) {
							uim.logException(e, true);
						}
				
						try {
							sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						nodeSyncedMsg = isSolidSubtangleUpdated(api);
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
							if(buildNodeAddress(api).equals(nodeList.get(0))) {
								boolean inconsistentTipsPairsError = nodeSyncedMsg.contains("inconsistent");
								uim.logWrn("waiting "+(inconsistentTipsPairsError ? 10 : 60)+" seconds for node '"+buildNodeAddress(api)+"' to get back in sync ("+nodeSyncedMsg+")");
								try {
									sleep(inconsistentTipsPairsError ? 10000 : 60000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							node = nodeList.get(0);
						} else {
							node = nodeList.get(0);
							nodeList.remove(0);
							nodeList.add(node);
						}
					}
				} while(!nodeSynced);
				availableAPIs++;
			}
		}.start();
		
	}
	
	public static int getApiIndex() {
		return apiIndex;
	}
	
	private static int getAPI() {
		if(availableAPIs < 1) {
			uim.logDbg("waiting until connection to any iota api is established"); 
			while(availableAPIs < 1) sleep(1000);
		}
		if(apis[apiIndex] == null) rotateAPI();
		int selectApiIndex = apiIndex;
		if(lastSyncCheck[selectApiIndex] + Configs.getInt(P.NODES_SYNC_CHECK_INTERVAL) < System.currentTimeMillis()/1000)
			reconnect(selectApiIndex);
		return selectApiIndex;
	}
	
	private static void rotateAPI() {
		if(availableAPIs < 1) {
			uim.logDbg("waiting until connection to any iota api is established"); 
			while(availableAPIs < 1) sleep(1000);
		}
		
		int tries = 0;
		do {
			apiIndex = (apiIndex+1)%apis.length;
			if(tries++ > apis.length) {
				uim.logWrn("no api available");
				sleep(10);
			}
		} while(apis[apiIndex] == null);
	}
	
	public static String[] findTransactionsByAddress(String address) {
		int api = getAPI();
		String[] addresses = {address};
		FindTransactionResponse findTransactionResponse = null;
		
		while(findTransactionResponse == null) {
			try {
				 findTransactionResponse = apis[api].findTransactionsByAddresses(addresses);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not check state of spam address '"+address+"'", e, api);
			}
		}
		
		return findTransactionResponse.getHashes();
	}
	
	public static boolean[] getInclusionStates(String[] hashes, String tip) {
		int api = getAPI();
		String[] tips = {tip};
		GetInclusionStateResponse getInclusionStateResponse = null;
		
		while(getInclusionStateResponse == null) {
			try {
				getInclusionStateResponse = apis[api].getInclusionStates(hashes, tips);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not check latest inclusion states", e, api);
			}
		}
		return getInclusionStateResponse.getStates();
	}
	
	public static String getLatestMilestone() {
		int api = getAPI();
		GetNodeInfoResponse getNodeInfoResponse = null;
		do {
			if(getNodeInfoResponse != null) {
				connectToNode(null, apiIndex, "latest milestone older than 10 minutes");
				sleep(5000);
			}
			getNodeInfoResponse = null;
			while(getNodeInfoResponse == null) {
				try {
					getNodeInfoResponse = apis[api].getNodeInfo();
				} catch (Throwable e) {
					api = handleThrowableFromIotaAPI("could not receive getNodeInfo", e, api);
				}
			}
		} while(findTractionsByHash(new String[] {getNodeInfoResponse.getLatestMilestone()}, api).get(0).getTimestamp() < System.currentTimeMillis()/1000-600);
		return getNodeInfoResponse.getLatestMilestone();
	}
	
	public static String isSolidSubtangleUpdated(int api) {
		
		GetNodeInfoResponse getNodeInfoResponse = null;
		try {
			getNodeInfoResponse = apis[api].getNodeInfo();
		} catch (Throwable e) {
			return e.getMessage() == null || e.getMessage().length() == 0 ? "not sure why though" : e.getMessage();
		}
		
		if(getNodeInfoResponse == null)
			return "did not receive response";
		
		if(Math.abs(getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex()-getNodeInfoResponse.getLatestMilestoneIndex()) > 3) {
			return "solid subtangle is not updated: lacking "+(getNodeInfoResponse.getLatestMilestoneIndex()-getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex())+" milestones behind";
		}
		
		String milestone = getNodeInfoResponse.getLatestSolidSubtangleMilestone();
		long secondsBehind = System.currentTimeMillis()/1000-findTractionsByHash(new String[] {milestone}, api).get(0).getTimestamp();
		
		if(secondsBehind > 600) {
			return "lacking "+(secondsBehind/60)+" minutes behind";
		}
		
		return null;
	}
	
	public static List<Transaction> findTractionsByHash(String[] hashes, int api) {
		List<Transaction> transactions = null;

		while(transactions == null) {
			try {
				transactions = apis[api].findTransactionsObjectsByHashes(hashes);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not find transactions", e, api);
			}
		}
		return transactions;
	}
	
	public static GetTransactionsToApproveResponse getTransactionsToApprove() {
		rotateAPI();
		int api = getAPI();
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;

		long timeStarted = System.currentTimeMillis();
		while(getTransactionsToApproveResponse == null) {
			try {
				UIManager.setSystemErrorEnabled(false);
				getTransactionsToApproveResponse = apis[api].getTransactionsToApprove(DEPTH);
				UIManager.setSystemErrorEnabled(true);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not get transactions to approve", e, api);
			}
		}
        totalTimeGetTxsToApprove += System.currentTimeMillis()-timeStarted;
        amountGetTxsToApprove++;
		return getTransactionsToApproveResponse;
	}
	
	public static void sendTransfer(List<Transfer> transfers, List<Input> inputs) {

		rotateAPI();
		int api = getAPI();
		
		SendTransferResponse sendTransferResponse = null;
		
		while(sendTransferResponse == null) {
			try {
				UIManager.setSystemErrorEnabled(false);
				sendTransferResponse = apis[api].sendTransfer("", 2, DEPTH, MIN_WEIGHT_MAGNITUDE, transfers, inputs, AddressManager.getSpamAddress(), false, false);
				UIManager.setSystemErrorEnabled(true);
			} catch (Throwable e) {
					api = handleThrowableFromIotaAPI("could not send transfer", e, api);
			}
		}
	}
	
	private static int handleThrowableFromIotaAPI(String failedAction, Throwable e, int i) {
		
		if(e.getMessage().contains("inconsistent")) {
			if(++inconsistentTipsPairs[i] >= INCONSISTENT_TIPS_PAIR_TOLERANCE) {
				inconsistentTipsPairs[i] = 0;
				connectToNode(null, i, "selected inconsistent tips pair "+INCONSISTENT_TIPS_PAIR_TOLERANCE+" times");
				rotateAPI();
				i = getAPI();
			}
			return i;
		}
			
		String errorMsg = e.getMessage();
		
		if(IllegalAccessError.class.isAssignableFrom(e.getClass()) || ArgumentException.class.isAssignableFrom(e.getClass()))
			errorMsg = failedAction + ": '" +
				(e.getMessage().contains("\"error\"") ? (e.getMessage().split("\"error\":\"")[1].split("\"")[0] + "'") : e.getMessage());
		else if (e.getMessage() == null || !e.getMessage().contains("Failed to connect to"))
			uim.logException(e, false);
		
		connectToNode(null, i, errorMsg);
		rotateAPI();
		return getAPI();
	}
	
	public static boolean isNodeSynced(int api) {
		
		uim.logDbg("checking if node is synced: '"+ buildNodeAddress(api) +"'");
		
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;
		
		try {
			UIManager.setSystemErrorEnabled(false);
			getTransactionsToApproveResponse = apis[api].getTransactionsToApprove(DEPTH);
			UIManager.setSystemErrorEnabled(true);
		}  catch (Throwable e) {
			return false;
		}
		if(getTransactionsToApproveResponse == null) return false; // it happens, for whatever reason?!
		String[] hashes = {getTransactionsToApproveResponse.getBranchTransaction(), getTransactionsToApproveResponse.getTrunkTransaction()};
		List<Transaction> transactions = findTractionsByHash(hashes, api);
		if(transactions.size() == 0)
			return false;
		long newerTimestamp = Math.max(transactions.get(0).getAttachmentTimestamp(), transactions.get(1).getAttachmentTimestamp());

		return newerTimestamp > System.currentTimeMillis() /1000-5*60;
	}
	
	public static String buildNodeListString() {
		String nodeListString = "";
		for(int i = 0; i < nodeList.size(); i++) {
			String node = nodeList.get(i);
			nodeListString += node + (i < nodeList.size()-1 ? ", " : "");
		}
		return nodeListString;
	}
	
	public static void addNode(String address, boolean log) {
		address = address.toLowerCase().replaceAll("/$", "");
		if(VALID_NODE_ADDRESS_REGEX.matcher(address).find()) {
			if(log) uim.logDbg("adding node to node list: '"+address+"'");
			nodeList.add(address);
		} else if(log) uim.logWrn("address is not correct: '" + address + "'");
		
	}
	
	public static void importRemoteNodeList() {
		String[] nodes = APIManager.downloadRemoteNodeLists();
		for(String node : nodes)
			addNode(node, false);
	}
	
	public static void addToNodeList(String nodeListString) {
		String[] nodes = (nodeListString.length() == 0) ? new String[0] : nodeListString.split("(,)");
		for(String node : nodes)
			addNode(node, true);
	}
	
	public static void shuffleNodeList() {
		Collections.shuffle(nodeList);
	}
	
	public static void reconnect(int api) {
		connectToNode(buildNodeAddress(api), api, null);
		lastSyncCheck[api] = System.currentTimeMillis()/1000;
	}
	
	private static String buildNodeAddress(int i) {
		return (apis[i] == null ? "" : apis[i].getProtocol() + "://" + apis[i].getHost() + ":" + apis[i].getPort());
	}
	
	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			uim.logException(e, true);
		}
	}
	
	public static UIManager getUIM() {
		return uim;
	}
    
    public static double getAvgTxsToApproveTime() {
		return amountGetTxsToApprove == 0 ? 0 : 0.001 * totalTimeGetTxsToApprove / amountGetTxsToApprove;
	}

	public static void broadcastAndStore(String[] trytes) {
		
		int api = getAPI();
		StoreTransactionsResponse storeTransactionsResponse = null;
		
		while(storeTransactionsResponse == null) {
			try {
				storeTransactionsResponse = apis[api].broadcastAndStore(trytes);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not check latest inclusion states", e, api);
			}
		}
	}

	public static void sendSpam() {
		
		int api = getAPI();
		
		while(true) {
			try {
				apis[api].sendSpam();
				return;
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not check latest inclusion states", e, api);
			}
		}
	}
}
