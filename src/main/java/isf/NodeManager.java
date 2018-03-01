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
import jota.dto.response.StoreTransactionsResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

public class NodeManager {

	private static final UIManager uim = new UIManager("NodeMngr");
	
	private static final int DEPTH = 4, INCONSISTENT_TIPS_PAIR_TOLERANCE = 20, TIP_AGE_TOLERANCE = 180, CONNECTING_DURATION_TOLERACE = 5, NODEINFO_DURATION_TOLERANCE = 3;
	
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.-]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	
	private static ArrayList<String> nodeList = null;
	
	private static int amountGetTxsToApprove = 0;
	private static long totalTimeGetTxsToApprove = 0;
	private static IotaAPI[] apis;
	private static boolean[] available;
	private static int[] inconsistentTipsPairs;
	private static long[] lastSyncCheck;
	private static int apiIndex = 0;
	private static int availableAPIs = 0;
	private static int nodeIndex = 0;
	
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
		loadNodeList();
		shuffleNodeList();
		
		apis = new IotaAPI[Math.min(Configs.getInt(P.NODES_AMOUNT_ROTATION), nodeList.size())];
		available = new boolean[apis.length];
		lastSyncCheck = new long[apis.length];
		inconsistentTipsPairs = new int[apis.length];
		
		for(int i = 0; i < apis.length; i++)
			connectToAnyNode(i, null);
		
		if(Configs.getBln(P.NODES_THIRD_PARTY_NODE_LIST)) TimeManager.addTask(new Task(30*60*1000, false) {
			@Override void onCall() { loadNodeList(); }
		});
	}

	private static boolean connectToNode(final String node, final int api) {
		
		TimeBomb t = new TimeBomb("connecting to node", 1) {
			
			@Override
			boolean onCall() {
				
				String protocol = node.split(":")[0];
				String host = node.split(":")[1].replace("//", "");
				String port = node.split(":")[2];
				
				apis[api] = (IotaAPI) new IotaAPI.Builder()
				        .protocol(protocol)
				        .host(host)
				        .port(port)
				        .localPoW(new GoldDiggerLocalPoW())
				        .build();
				
				return true;
			}
		};
		
		if(!t.call(CONNECTING_DURATION_TOLERACE)) return false;
		
		String nodeSyncedMsg = isSolidSubtangleUpdated(api);
		if(nodeSyncedMsg != null)
			uim.logDbg("node '" + buildNodeAddress(api) + "' ["+api+"] is not synced ("+nodeSyncedMsg+"), changing node");
		return nodeSyncedMsg == null;
	}

	public static void connectToAnyNode(final int api, final String parNodeSyncedMsg) {
		available[api] = false;
		
		if(parNodeSyncedMsg != null)
			uim.logDbg("node '" + buildNodeAddress(api) + "' ["+api+"] is not synced ("+parNodeSyncedMsg+"), changing node");
		
		new Thread() {
			@Override
			public void run() {
				while(!connectToNode(getNextNode(), api)) {
					if(nodeList.size() == 1) {
						uim.logWrn("your only node '"+buildNodeAddress(api)+"' is not synced, waiting 30s and try again (add more nodes for higher reliability)");
						NodeManager.sleep(30000);
					} else {
						NodeManager.sleep(3000);
					}
				}
				
				lastSyncCheck[api] = System.currentTimeMillis()/1000;
				available[api] = true;
				
				int newAvailableAPIs = 0;
				for(boolean a : available) if(a) newAvailableAPIs++;
				availableAPIs = newAvailableAPIs;
				
				if(availableAPIs == 1) apiIndex = api;
			}
		}.start();
		
	}
	
	private static String getNextNode() {
		if(nodeList.size() == 0)
			try {
				throw new Exception("could not connect to node: node list empty");
			} catch (Exception e) {
				uim.logException(e, true);
				return null;
			}
		else {
			nodeIndex = (nodeIndex+1)%nodeList.size();
			return nodeList.get(nodeIndex);
		}
	}
	

	private static int getRotatedAPI() {
		rotateAPI();
		return getAPI();
	}
	
	public static int getApiIndex() {
		return apiIndex;
	}
	
	private static int getAPI() {
		if(availableAPIs < 1) {
			uim.logDbg("waiting until connection to any iota api is established"); 
			while(availableAPIs < 1) sleep(1000);
		}
		if(!available[apiIndex]) rotateAPI();
		int selectApiIndex = apiIndex;
		if(lastSyncCheck[selectApiIndex] + Configs.getInt(P.NODES_SYNC_CHECK_INTERVAL) < System.currentTimeMillis()/1000)
			doSyncCheck(selectApiIndex);
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
		} while(!available[apiIndex]);
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
				connectToAnyNode(apiIndex, "latest milestone older than 10 minutes");
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
		} while(findTractionsByHashes(new String[] {getNodeInfoResponse.getLatestMilestone()}, api).get(0).getTimestamp() < System.currentTimeMillis()/1000-600);
		return getNodeInfoResponse.getLatestMilestone();
	}
	
	public static GetNodeInfoResponse getNodeInfo(int api) {
		
		final ObjectCarrier oc = new ObjectCarrier();
		
		TimeBomb tb = new TimeBomb("requesting node info", 0) {
			
			@Override
			boolean onCall() {
				oc.o = apis[api].getNodeInfo();
				return true;
			}
		};
		
		tb.call(NODEINFO_DURATION_TOLERANCE);
		return (GetNodeInfoResponse) oc.o;
	}
	
	public static String isSolidSubtangleUpdated(int api) {
		
		GetNodeInfoResponse getNodeInfoResponse = null;
		try {
			getNodeInfoResponse = getNodeInfo(api);
		} catch (Throwable e) {
			return e.getMessage() == null || e.getMessage().length() == 0 ? "not sure why though" : e.getMessage();
		}
		
		if(getNodeInfoResponse == null)
			return "did not receive getNodeInfoResponse response within "+NODEINFO_DURATION_TOLERANCE+" seconds";
		
		if(Math.abs(getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex()-getNodeInfoResponse.getLatestMilestoneIndex()) > 3) {
			return "solid subtangle is not updated: lacking "+(getNodeInfoResponse.getLatestMilestoneIndex()-getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex())+" milestones behind";
		}
		
		String milestone = getNodeInfoResponse.getLatestSolidSubtangleMilestone();
		long secondsBehind = System.currentTimeMillis()/1000-findTractionsByHashes(new String[] {milestone}, api).get(0).getTimestamp();
		
		if(secondsBehind > 600) {
			return "lacking "+(secondsBehind/60)+" minutes behind";
		}
		
		return null;
	}
	
	public static List<Transaction> findTractionsByHashes(String[] hashes, int api) {
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
		int api = getRotatedAPI();
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;

		long timeStarted = System.currentTimeMillis();
		while(getTransactionsToApproveResponse == null) {
			try {
				getTransactionsToApproveResponse = apis[api].getTransactionsToApprove(DEPTH);
			} catch(IllegalStateException e) {
				if(e.getMessage().contains("thread interrupted")) {}
				else api = handleThrowableFromIotaAPI("could not get transactions to approve", e, api);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not get transactions to approve", e, api);
			}
		}
        totalTimeGetTxsToApprove += System.currentTimeMillis()-timeStarted;
        amountGetTxsToApprove++;
		return getTransactionsToApproveResponse;
	}
	
	private static int handleThrowableFromIotaAPI(String failedAction, Throwable e, int i) {
		
		if(e.getMessage().contains("inconsistent")) {
			if(++inconsistentTipsPairs[i] >= INCONSISTENT_TIPS_PAIR_TOLERANCE) {
				inconsistentTipsPairs[i] = 0;
				connectToAnyNode(i, "selected inconsistent tips pair "+INCONSISTENT_TIPS_PAIR_TOLERANCE+" times");
			}
			return getRotatedAPI();
		}
			
		String errorMsg = e.getMessage();
		
		if(IllegalAccessError.class.isAssignableFrom(e.getClass()) || ArgumentException.class.isAssignableFrom(e.getClass()))
			errorMsg = failedAction + ": '" +
				(e.getMessage().contains("\"error\"") ? (e.getMessage().split("\"error\":\"")[1].split("\"")[0] + "'") : e.getMessage());
		else if (e.getMessage() == null || !e.getMessage().contains("Failed to connect to"))
			uim.logException(e, false);
		
		connectToAnyNode(i, errorMsg);
		return getRotatedAPI();
	}
	
	private static boolean isNodeSynced(int api) {
		
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
		List<Transaction> transactions = findTractionsByHashes(hashes, api);
		if(transactions.size() == 0)
			return false;
		long newerTimestamp = Math.max(transactions.get(0).getAttachmentTimestamp(), transactions.get(1).getAttachmentTimestamp());

		lastSyncCheck[api] = System.currentTimeMillis();
		return newerTimestamp > System.currentTimeMillis() /1000-TIP_AGE_TOLERANCE;
	}
	
	public static String buildNodeListString() {
		String nodeListString = "";
		for(int i = 0; i < nodeList.size(); i++) {
			String node = nodeList.get(i);
			nodeListString += node + (i < nodeList.size()-1 ? ", " : "");
		}
		return nodeListString;
	}
	
	public static void addNode(ArrayList<String> nodeList, String address, boolean log) {
		address = address.toLowerCase().replaceAll("/$", "");
		if(VALID_NODE_ADDRESS_REGEX.matcher(address).find()) {
			if(log) uim.logDbg("adding node to node list: '"+address+"'");
			nodeList.add(address);
		} else if(log) uim.logWrn("address is not correct: '" + address + "'");
	}
	
	public static void addNode(String address, boolean log) {
		addNode(nodeList, address, log);
	}
	
	public static void importRemoteNodeList(ArrayList<String> nodeList) {
		String[] nodes = APIManager.downloadRemoteNodeLists();
		for(String node : nodes)
			addNode(nodeList, node, false);
	}
	
	public static void addToNodeList(ArrayList<String> parNodeList, String nodeListString) {
		String[] nodes = (nodeListString.length() == 0) ? new String[0] : nodeListString.split("(,)");
		for(String node : nodes)
			addNode(parNodeList, node, true);
	}
	
	private static void shuffleNodeList() {
		Collections.shuffle(nodeList);
	}
	
	private static void doSyncCheck(int api) {
		new Thread() {
			@Override
			public void run() {
				String isSolidSubtangleUpdated = isSolidSubtangleUpdated(api);
				if(isSolidSubtangleUpdated != null || !isNodeSynced(api))
					connectToAnyNode(api, isSolidSubtangleUpdated != null ? isSolidSubtangleUpdated : "returned tips are older than " + TIP_AGE_TOLERANCE + " seconds");
				else lastSyncCheck[api] = System.currentTimeMillis()/1000;
			}
		}.start();
	}
	
	private static String buildNodeAddress(int i) {
		return (apis[i] == null ? "" : apis[i].getProtocol() + "://" + apis[i].getHost() + ":" + apis[i].getPort());
	}
	
	private static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			
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
		
		int api = getRotatedAPI();
		
		while(true) {
			try {
				apis[api].sendSpam();
				return;
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("could not send spam transaction", e, api);
			}
		}
	}
	
	public static void loadNodeList() {
		uim.logDbg("downloading remote node list");
		ArrayList<String> newNodeList = new ArrayList<>();
		addToNodeList(newNodeList, Configs.get(P.NODES_LIST).replace(" ", ""));
		if(Configs.getBln(P.NODES_THIRD_PARTY_NODE_LIST)) NodeManager.importRemoteNodeList(newNodeList);
		nodeList = newNodeList;
		
		shuffleNodeList();
		uim.logDbg("node list includes " + nodeList.size() + " nodes");
	}
}

class ObjectCarrier {
	Object o;
}
