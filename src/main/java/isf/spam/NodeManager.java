package isf.spam;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import isf.APIManager;
import isf.Configs;
import isf.FileManager;
import isf.P;
import isf.logic.CronJob;
import isf.logic.CronJobManager;
import isf.logic.ObjectWrapper;
import isf.logic.TimeAbortCall;
import isf.ui.R;
import isf.ui.UIManager;
import iota.GOldDiggerLocalPoW;
import iota.IotaAPI;
import jota.dto.response.FindTransactionResponse;
import jota.dto.response.GetInclusionStateResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.dto.response.StoreTransactionsResponse;
import jota.model.Transaction;

public class NodeManager {

	private static final UIManager uim = new UIManager("NodeMngr");
	private static int DEPTH = 1;
	
	private static final int INCONSISTENT_TIPS_PAIR_TOLERANCE = 20, TIP_AGE_TOLERANCE = 180, CONNECTING_DURATION_TOLERACE = 6, NODEINFO_DURATION_TOLERANCE = 6;
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.-]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	
	private static ArrayList<String> nodeList = null;
	private static int nodeIndex = 0;
	
	private static int amountGetTxsToApprove = 0;
	private static long totalTimeGetTxsToApprove = 0;
	
	private static IotaAPI[] apis;
	private static boolean[] available;
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
		loadNodeList();
		shuffleNodeList();
		
		apis = new IotaAPI[Math.min(Configs.getInt(P.NODES_AMOUNT_ROTATION), nodeList.size())];
		available = new boolean[apis.length];
		lastSyncCheck = new long[apis.length];
		inconsistentTipsPairs = new int[apis.length];
		
		for(int i = 0; i < apis.length; i++)
			connectToAnyNode(i, null);
		
		if(Configs.getBln(P.NODES_THIRD_PARTY_NODE_LIST)) CronJobManager.addCronJob(new CronJob(30*60000, false, false) { @Override public void onCall() { loadNodeList(); } });
		
		DEPTH = Configs.getInt(P.SPAM_DEPTH);
	}

	private static boolean connectToNode(final String node, final int api) {
		
		TimeAbortCall t = new TimeAbortCall(R.STR.getString("nodes_action_connecting"), 1) {
			
			@Override
			public boolean onCall() {
				
				String protocol = node.split(":")[0];
				String host = node.split(":")[1].replace("//", "");
				String port = node.split(":")[2];
				
				apis[api] = (IotaAPI) new IotaAPI.Builder()
				        .protocol(protocol)
				        .host(host)
				        .port(port)
				        .localPoW(new GOldDiggerLocalPoW())
				        .build();
				
				return true;
			}
		};
		
		if(!t.call(CONNECTING_DURATION_TOLERACE)) return false;
		
		String isNodeSynced = isNodeSynced(api);
		if(isNodeSynced != null)
			uim.logDbg(String.format(R.STR.getString("nodes_action_changing"), buildNodeAddress(api), api, isNodeSynced));
		return isNodeSynced == null;
	}

	public static void connectToAnyNode(final int api, final String parNodeSyncedMsg) {
		available[api] = false;
		
		if(parNodeSyncedMsg != null)
            uim.logDbg(String.format(R.STR.getString("nodes_action_changing"), buildNodeAddress(api), api, parNodeSyncedMsg));
		
		new Thread("connectToAnyNode("+api+")") {
			@Override
			public void run() {
				while(!connectToNode(getNextNode(), api)) {
					if(nodeList.size() == 1) {
						uim.logWrn(String.format(R.STR.getString("nodes_not_synced"), nodeList.get(0), 30));
						NodeManager.sleep(30000);
					} else {
						NodeManager.sleep(3000);
					}
				}
				
				available[api] = true;
				
				int newAvailableAPIs = 0;
				for(boolean a : available) if(a) newAvailableAPIs++;
				availableAPIs = newAvailableAPIs;
				
				if(availableAPIs == 1) apiIndex = api;
			}
		}.start();
		
	}
	
	private static String getNextNode() {
		if(nodeList.size() == 0) {
		    uim.logErr(R.STR.getString("nodes_node_list_empty"));
		    System.exit(0);
        }

        nodeIndex = (nodeIndex+1)%nodeList.size();
        return nodeList.get(nodeIndex);
	}
	

	public static int getRotatedAPI() {
		rotateAPI();
		return getAPI();
	}
	
	public static int getApiIndex() {
		return apiIndex;
	}
	
	private static int getAPI() {
		if(availableAPIs < 1) {
			uim.logDbg(R.STR.getString("nodes_waiting"));
			while(availableAPIs < 1) sleep(500);
		}
		if(!available[apiIndex]) rotateAPI();
		int selectApiIndex = apiIndex;
		if(lastSyncCheck[selectApiIndex] + Configs.getInt(P.NODES_SYNC_CHECK_INTERVAL) < System.currentTimeMillis()/1000)
			doSyncCheck(selectApiIndex);
		return selectApiIndex;
	}
	
	private static void rotateAPI() {
		if(availableAPIs < 1) {
            uim.logDbg(R.STR.getString("nodes_waiting"));
			while(availableAPIs < 1) sleep(500);
		}
		
		int tries = 0;
		do {
			apiIndex = (apiIndex+1)%apis.length;
			if(tries++ > apis.length) {
				uim.logWrn(R.STR.getString("nodes_unavailable"));
				sleep(5000);
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
				api = handleThrowableFromIotaAPI(String.format(R.STR.getString("nodes_action_check_address"), address), e, api);
			}
		}
		
		return findTransactionResponse.getHashes();
	}
	
	public static boolean[] getLatestInclusion(String[] hashes) {
		int api = getAPI();
		GetInclusionStateResponse getInclusionStateResponse = null;
		
		while(getInclusionStateResponse == null) {
			try {
				getInclusionStateResponse = apis[api].getLatestInclusion(hashes);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI(R.STR.getString("nodes_action_check_inclusions"), e, api);
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
				api = getRotatedAPI();
			}
			getNodeInfoResponse = getNodeInfo(api, true);
		} while(findTractionsByHashes(new String[] {getNodeInfoResponse.getLatestMilestone()}, api).get(0).getTimestamp() < System.currentTimeMillis()/1000-600);
		return getNodeInfoResponse.getLatestMilestone();
	}
	
	public static GetNodeInfoResponse getNodeInfo(final int parApi, boolean tryMultipleTimes) {

		final ObjectWrapper api = new ObjectWrapper(parApi);
		final ObjectWrapper res = new ObjectWrapper(null);
		
		TimeAbortCall tb = new TimeAbortCall("requesting node info", 0) {
			@Override
			public boolean onCall() {
				try {
					res.o = apis[(int)api.o].getNodeInfo();
					return true;
				} catch (Throwable e) {
					api.o = handleThrowableFromIotaAPI("receive getNodeInfo", e, (int)api.o);
					return false;
				}
			}
		};
		
		do { tb.call(NODEINFO_DURATION_TOLERANCE); } while(tryMultipleTimes);
		return (GetNodeInfoResponse) res.o;
	}
	
	public static List<Transaction> findTractionsByHashes(String[] hashes, int api) {
		List<Transaction> transactions = null;

		while(transactions == null) {
			try {
				transactions = apis[api].findTransactionsObjectsByHashes(hashes);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("find transactions", e, api);
			}
		}
		return transactions;
	}
	
	public static boolean isAvailable(int api) {
		return available[api];
	}
	
	public static GetTransactionsToApproveResponse getTransactionsToApprove(int api) {
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;

		long timeStarted = System.currentTimeMillis();
		while(getTransactionsToApproveResponse == null) {
			try {
				if(!available[api]) return null;
				getTransactionsToApproveResponse = apis[api].getTransactionsToApprove(DEPTH);
			} catch(IllegalStateException e) {
				if(e.getMessage().contains("thread interrupted")) {}
				else api = handleThrowableFromIotaAPI("get transactions to approve", e, api);
			} catch (Throwable e) {
				api = handleThrowableFromIotaAPI("get transactions to approve", e, api);
			}
		}
        totalTimeGetTxsToApprove += System.currentTimeMillis()-timeStarted;
        amountGetTxsToApprove++;
		return getTransactionsToApproveResponse;
	}
	
	private static int handleThrowableFromIotaAPI(String failedAction, Throwable e, int i) {
		
		if(e.getMessage()!= null && e.getMessage().contains("inconsistent")) {
			if(++inconsistentTipsPairs[i] >= INCONSISTENT_TIPS_PAIR_TOLERANCE) {
				inconsistentTipsPairs[i] = 0;
				connectToAnyNode(i, "selected inconsistent tips pair "+INCONSISTENT_TIPS_PAIR_TOLERANCE+" times");
			}
			return getRotatedAPI();
		}
		
		String errorClassName = e.getClass().getName();
		String errorMessage =
                e.getMessage() == null ? " <null>" :
                (e.getMessage().contains("\"error\"") ? (e.getMessage().split("\"error\":\"")[1].split("\"")[0] + "'") : e.getMessage());
		String error = "could not " + failedAction + ": " + errorClassName + " - " + errorMessage;
		
		connectToAnyNode(i, error);
		return getRotatedAPI();
	}
	
	private static String isNodeSynced(int api) {
		
		GetNodeInfoResponse getNodeInfoResponse = null;
		try {
			getNodeInfoResponse = getNodeInfo(api, false);
		} catch (Throwable e) {
			return e.getClass().getName() + ": " + e.getMessage();
		}
		
		if(getNodeInfoResponse == null)
			return "did not receive getNodeInfoResponse response within "+NODEINFO_DURATION_TOLERANCE+" seconds";
		
		if(Math.abs(getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex()-getNodeInfoResponse.getLatestMilestoneIndex()) > 3)
			return "solid subtangle is not updated: lacking "+(getNodeInfoResponse.getLatestMilestoneIndex()-getNodeInfoResponse.getLatestSolidSubtangleMilestoneIndex())+" milestones behind";
		
		String milestone = getNodeInfoResponse.getLatestSolidSubtangleMilestone();
		long secondsBehind = System.currentTimeMillis()/1000-findTractionsByHashes(new String[] {milestone}, api).get(0).getTimestamp();
		if(secondsBehind > 600)
			return "lacking "+(secondsBehind/60)+" minutes behind";
		
		GetTransactionsToApproveResponse getTransactionsToApproveResponse = null;
		
		try {
			getTransactionsToApproveResponse = apis[api].getTransactionsToApprove(DEPTH);
		}  catch (Throwable e) {
			return e.getClass().getName() + ": " + e.getMessage();
		}
		
		if(getTransactionsToApproveResponse == null) return "getTransactionsToApproveResponse == null";
		
		String[] hashes = {getTransactionsToApproveResponse.getBranchTransaction(), getTransactionsToApproveResponse.getTrunkTransaction()};
		List<Transaction> transactions = findTractionsByHashes(hashes, api);
		if(transactions.size() == 0) return "silly node pretends to not know tips it just provided";
		long newerTimestamp = Math.max(transactions.get(0).getAttachmentTimestamp(), transactions.get(1).getAttachmentTimestamp());
		long tipAge = System.currentTimeMillis() /1000-newerTimestamp;
		
		lastSyncCheck[api] = System.currentTimeMillis();
		return tipAge > TIP_AGE_TOLERANCE ? "tips are "+tipAge+"s old, tolerance is set to " + TIP_AGE_TOLERANCE + "s" : null;
	}
	
	public static String buildNodeListString() {
		String nodeListString = "";
		for(int i = 0; i < nodeList.size(); i++) {
			String node = nodeList.get(i);
			nodeListString += node + (i < nodeList.size()-1 ? "\n" : "");
		}
		return nodeListString;
	}
	
	public static void addNode(ArrayList<String> nodeList, String address, boolean log) {
		address = address.toLowerCase().replaceAll("/$", "");
		if(address.length() == 0 || address.charAt(0) == '#') return;
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
		String[] nodes = (nodeListString.length() == 0) ? new String[0] : nodeListString.split("\n");
		for(String node : nodes)
			addNode(parNodeList, node, true);
	}
	
	private static void shuffleNodeList() {
		Collections.shuffle(nodeList);
	}
	
	private static void doSyncCheck(final int api) {
		new Thread("doSyncCheck("+api+")") {
			@Override
			public void run() {
				String error = isNodeSynced(api);
				if(error != null) connectToAnyNode(api, error);
			}
		}.start();
	}
	
	private static String buildNodeAddress(int i) {
		return (apis[i] == null ? "" : apis[i].getProtocol() + "://" + apis[i].getHost() + ":" + apis[i].getPort());
	}
	
	private static void sleep(int ms) {
		try { Thread.sleep(ms); } catch (InterruptedException e) { }
	}
	
	public static UIManager getUIM() {
		return uim;
	}
    
    public static double getAvgTxsToApproveTime() {
		return amountGetTxsToApprove == 0 ? 0 : 0.001 * totalTimeGetTxsToApprove / amountGetTxsToApprove;
	}

	public static void broadcastAndStore(String trytes) throws InterruptedException {
		
		int api = getAPI();
		StoreTransactionsResponse storeTransactionsResponse = null;
		
		while(storeTransactionsResponse == null) {
			try {
				storeTransactionsResponse = apis[api].broadcastAndStore(trytes);
			} catch (Throwable e) {
				if(e.getMessage().contains("thread interrupted"))
					throw(new InterruptedException());
				api = handleThrowableFromIotaAPI("broadcast transaction", e, api);
			}
		}
	}

	public static boolean sendSpam() {
		
		int api = getRotatedAPI();
		
		try {
			apis[api].sendSpam();
			return true;
		} catch (Throwable e) {
			api = handleThrowableFromIotaAPI("send spam transaction", e, api);
			return false;
		}
	}
	
	public static void loadNodeList() {
		uim.logDbg(R.STR.getString("nodes_download_remote"));
		ArrayList<String> newNodeList = new ArrayList<>();

		if(!FileManager.exists("nodelist.cfg"))
		    FileManager.write("nodelist.cfg", buildNodesFileHeader());

        String nodelist = FileManager.read("nodelist.cfg");
		addToNodeList(newNodeList, nodelist.replace(" ", ""));
		if(Configs.getBln(P.NODES_THIRD_PARTY_NODE_LIST)) NodeManager.importRemoteNodeList(newNodeList);
		nodeList = newNodeList;
		
		shuffleNodeList();
		if(nodeList.size() > 0)
            uim.logDbg("node list includes " + nodeList.size() + " nodes");
        else {
            uim.logErr(R.STR.getString("nodes_node_list_empty"));
            System.exit(0);
        }
	}

    public static String buildNodesFileHeader() {
        return "# " + String.format(R.STR.getString("nodes_file_header"),
                R.URL.getString("node_format"),
                R.URL.getString("node_example_1"),
                R.URL.getString("node_example_2")) + "\n\n";
    }
}