package isf;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import isf.ui.UIManager;

public class APIManager {
	
	private static final UIManager uim = new UIManager("API-Mngr");

	private static final String SPAM_FUND_API_URL = "http://mikrohash.de/isf/api/"+Main.getVersion()+"/";
	private static final String NODE_LIST_IOTANODEHOST = "http://mikrohash.de/isf/api/nodelist/iotanodehost.json";
	private static final String NODE_LIST_IOTANODESNET = "http://mikrohash.de/isf/api/nodelist/iotanodesnet.json";
	public static final String CMC_API_IOTA = "https://api.coinmarketcap.com/v1/ticker/iota/";

	public static String request(String urlString, String data) {
		try {
			URL url = new URL(urlString);
		    HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			if(data != null) {
			    con.setRequestMethod(data == null ? "GET" : "POST");
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.writeBytes(data == null ? "" : data);
				wr.flush();
				wr.close();
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())); // <--
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null)
				response.append(inputLine);
			
			in.close();
			
			return response.toString();
			
		} catch (IOException e) {
			uim.logWrn("problem communicating with " + urlString);
			uim.logException(e, false);
		}
		return "";
	}
	
	public static JSONObject requestUpdates() {
		return keepSendingUntilSuccess("updates", "", "requesting updates");
	}
	
	public static JSONObject requestBalance() {
		return keepSendingUntilSuccess("balance", "", "requesting reward balance");
	}
	
	public static int requestCommand() {
		return keepSendingUntilSuccess("command", "", "requesting remote command").getInt("command");
	}

	public static JSONObject requestSpamParameters() {
		return keepSendingUntilSuccess("address", "", "requesting spam address");
	}
	
	public static JSONArray requestRewards() {
		return keepSendingUntilSuccess("rewards", "", "requesting reward stats").getJSONArray("tails");
	}

	public static void printRewards() {
		final String[] STATES = {"REJECTED", "PENDING ", "FINISHED", "DELAYED "};
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		DecimalFormat df = new DecimalFormat("###,###,##0");
		
		JSONArray tails = requestRewards();
		if(tails.length() > 0) {
			uim.print("Here comes a list of your most recent spam addresses:\n");

			uim.print(" > REJECTED < sometimes addresses will be rejected at first, but they will usually be accepted within hours");
			uim.print(" > FINISHED < addresses have received all rewards");
			uim.print(" > PENDING  < addresses haven't been checked for confirmations yet, check back in 5 minutes");
			uim.print(" > DELAYED  < means it is delayed for 30 minutes for best confirmation rates\n");
			
			uim.print("  ID   |  STATE   |  REWARD  |  CNFRMED |  TIME PUBLISHED     |  ADDRESS TAIL / END OF ADDRESS");
			uim.print("=======|==========|==========|==========|=====================|=========================================");
			for(int i = 0; i < tails.length(); i++) {
				JSONObject obj = tails.getJSONObject(i);

				uim.print(UIManager.padLeft("#"+obj.getInt("id"), 6) + " | "
						+ STATES[obj.getInt("state")+1] + " | "
						+ UIManager.padLeft(df.format(obj.getInt("balance"))+"", 6) + " i |"
						+ UIManager.padLeft(obj.getInt("confirmed") + "", 5) + " txs | "
						+ sdf.format(obj.getLong("created")*1000) + " | "
						+ obj.getString("trytes"));
			}
		} else {
			uim.logWrn("You haven't received any rewards yet. Keep spamming!");
		}
		uim.print("\nYou have a total account balance of " + df.format(APIManager.requestBalance().getInt("balance")) + " iotas. You can withdraw here: http://iotaspam.com/withdraw");
	}
	
	public static void broadcastTail(Tail tail) {
		keepSendingUntilSuccess("savetail", "trytes="+tail.getTrytes()+"&total="+tail.getTotalTxs()+"&confirmed="+tail.getConfirmedTxs(), "uploading address state");
	}
	
	public static JSONObject keepSendingUntilSuccess(String filename, String data, String action) {

		data = data == null ? "" : data;
		
		if(Configs.isf_email == null || Configs.isf_password == null)
			Configs.askForAccountData(false);
			
		long nonce = getTimeStamp();
		int timeOutInSeconds = 5;
		
		do {
			String hash = md5((Configs.isf_password+"-"+nonce).getBytes());
			String jsonString = request(SPAM_FUND_API_URL + filename+".php", "email="+Configs.isf_email+"&nonce="+nonce+"&hash="+hash+"&build="+Main.getBuild()+(data.length() > 0 ? "&"+data : ""));
			String error;
			int errorId = -1;
			try {
				JSONObject obj = new JSONObject(jsonString);
				if(obj.getBoolean("success")) return obj;
				error = obj.getString("error");
				if(obj.has("error_id")) errorId = obj.getInt("error_id");
				nonce = obj.getLong("nonce")+timeOutInSeconds;
			} catch(JSONException e) {
				error = "error is probably caused by our API, write us at contact@iotaspam.com if it persists: '"+e.getMessage()+"'";
				uim.logDbg("invalid json: " + jsonString);
				nonce = getTimeStamp()+timeOutInSeconds;
			}
			if(!filename.equals("signin") || errorId == -1) {
				uim.logWrn(action + " failed ("+error+"), trying again in "+timeOutInSeconds+" seconds");
				sleep(timeOutInSeconds * 1000);
				timeOutInSeconds = Math.min(timeOutInSeconds*2, 120);
			} else {
				uim.logWrn(action + " failed ("+error+"), please reenter your account data");
				Configs.isf_email = null;
				Configs.askForAccountData(false);
				timeOutInSeconds = 5;
			}
		} while (true);
	}
	
	private static String md5(byte[] s) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			uim.logException(e, true);
		}
		return bytesToHex(md.digest(s)).toLowerCase();
	}
	
	public static String[] downloadRemoteNodeLists() {
		JSONArray arr1 = null, arr2 = null;
		int tries = 3;
		
		while(arr1 == null && tries-- > 0) {
			try {
				arr1 = new JSONArray(request(NODE_LIST_IOTANODEHOST, ""));
			} catch(Throwable t) {
				if(tries == 0) {
					uim.logWrn("tried three times to download nodes from " + NODE_LIST_IOTANODEHOST + " without success");
					uim.logException(t, false);
				} else {
					sleep(3000);
				}
				arr1 = null;
			}
		}

		tries = 3;
		while(arr2 == null && tries-- > 0) {
			try {
				arr2 = new JSONArray(request(NODE_LIST_IOTANODESNET, ""));
			} catch(Throwable t) {
				if(tries == 0) {
					uim.logWrn("tried three times to download nodes from " + NODE_LIST_IOTANODESNET + " without success");
					uim.logException(t, false);
				} else {
					sleep(3000);
				}
				arr2 = null;
			}
		}
		
		int l1 = arr1 == null ? 0 : arr1.length(),
				l2 = arr2 == null ? 0 : arr2.length();
		
		String[] nodes = new String[l1+l2];
		for(int i = 0; i < l1; i++)
			nodes[i] = arr1.getJSONObject(i).getString("host");
		for(int i = 0; i < l2; i++)
			nodes[arr1.length()+i] = "http://"+arr2.getJSONObject(i).getString("hostname")+":"+arr2.getJSONObject(i).getInt("port");
		return nodes;
	}
	
	private static String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	private static long getTimeStamp() {
		return System.currentTimeMillis()/1000;
	}
	
	private static void sleep(int ms) {
		try { Thread.sleep(ms); }
		catch (InterruptedException e) { e.printStackTrace(); }
	}
}