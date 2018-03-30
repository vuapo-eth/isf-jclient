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

import isf.spam.AddressManager;
import isf.spam.SpamThread;
import isf.spam.Tail;
import isf.ui.R;
import isf.ui.UIQuestion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import isf.ui.UIManager;

public class APIManager {
	
	private static final UIManager uim = new UIManager("API-Mngr");

	private static final String SPAM_FUND_API_URL = String.format(R.URL.getString("spam_fund_api"), Main.getVersion());
	private static final String THIRD_PARTY_NODE_LIST = R.URL.getString("node_list");;
	public static final String CMC_API_IOTA = R.URL.getString("cmc_iota_ticker");
    private static String isf_email = null, isf_password = null;

    public static void login(String email, String password) {
        isf_email = email;
        isf_password = password;

        if(!Main.isInOnlineMode()) return;

        if(isf_email == null || isf_password == null)
            askForAccountData();

        uim.logDbg(String.format(R.STR.getString("api_signin"), isf_email));
        APIManager.keepSendingUntilSuccess("signin", null, R.STR.getString("api_action_signing_in"));

        if(email == null || password == null)
            uim.print(String.format(R.STR.getString("api_signin_parameter"), UIManager.ANSI_BOLD+"java -jar isf-jclient -email bob@gmail.com -pass rsdKlVKPan17"+UIManager.ANSI_RESET));
    }

	public static String request(final String urlString, String data) {
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
			uim.logDbg(urlString);
			uim.logWrn(String.format(R.STR.getString("api_communication_failed"),  urlString, e.getMessage()));
			return null;
		}
	}
	
	public static JSONObject requestUpdates() {
		return keepSendingUntilSuccess("updates", "", R.STR.getString("api_action_requesting_updates"));
	}
	
	public static JSONObject requestBalance() {
		return keepSendingUntilSuccess("balance", "", R.STR.getString("api_action_requesting_balance"));
	}
	
	public static JSONObject requestCommand() {
		return keepSendingUntilSuccess("command", "", R.STR.getString("api_action_requesting_command"));
	}

	public static void requestSpamParameters() {
		if(Main.isInOnlineMode()) {
            JSONObject spamParameters = keepSendingUntilSuccess("address", "", R.STR.getString("api_action_requesting_address"));
            AddressManager.setAddressBase(spamParameters.getString("address"));
            SpamThread.setTag(spamParameters.getString("tag"));
        } else {
            AddressManager.setAddressBase(Configs.get(P.SPAM_OFFLINE_ADDRESS));
            SpamThread.setTag(Configs.get(P.SPAM_OFFLINE_TAG));
        }
	}
	
	public static JSONArray requestRewards() {
		return keepSendingUntilSuccess("rewards", "", R.STR.getString("api_action_requesting_rewards")).getJSONArray("tails");
	}

	public static void printRewards() {
        if(!Main.isInOnlineMode()) {
            if((isf_email != null && isf_password != null) || uim.askForBoolean("since you are in offline mode, you will have to log in to see your rewards. do you want to proceed?")) {
                Main.setOnlineMode(true);
                printRewards();
                Main.setOnlineMode(false);
            }
            return;
        }

        JSONArray tails = requestRewards();

		final String[] STATES = {"REJECTED", "PENDING ", "FINISHED", "DELAYED "};
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		DecimalFormat df = new DecimalFormat("###,###,##0");
		if(tails.length() > 0) {
			uim.print("Here comes a list of your most recent spam addresses:\n");

			uim.print(" > REJECTED < sometimes addresses will be rejected at first, but they will usually be accepted within hours");
			uim.print(" > FINISHED < addresses have received all rewards");
			uim.print(" > PENDING  < addresses haven't been checked for confirmations yet, check back in 5 minutes");
			uim.print(" > DELAYED  < means it is delayed for 30 minutes for best confirmation rates\n");
			
			uim.print("  ID      |  STATE   |  REWARD  |  CNFRMED |  TIME PUBLISHED     |  ADDRESS TAIL / END OF ADDRESS");
			uim.print("==========|==========|==========|==========|=====================|=========================================");
			for(int i = 0; i < tails.length(); i++) {
				JSONObject obj = tails.getJSONObject(i);

				uim.print(UIManager.padLeft("#"+obj.getInt("id"), 9) + " | "
						+ STATES[obj.getInt("state")+1] + " | "
						+ UIManager.padLeft(df.format(obj.getInt("balance"))+"", 6) + " i |"
						+ UIManager.padLeft(obj.getInt("confirmed") + "", 5) + " txs | "
						+ sdf.format(obj.getLong("created")*1000) + " | "
						+ obj.getString("trytes"));
			}
		} else {
			uim.logWrn("You haven't received any rewards yet. Keep spamming!");
		}
		uim.print("\nYou have a total account balance of " + df.format(APIManager.requestBalance().getInt("balance")) + " iotas. You can withdraw here: " + R.URL.getString("spam_fund_withdraw"));
	}
	
	public static void broadcastTail(Tail tail) {
		keepSendingUntilSuccess("savetail", "trytes="+tail.getTrytes()+"&total="+tail.getTotalTxs()+"&confirmed="+tail.getConfirmedTxs(), R.STR.getString("api_action_uploading_address"));
	}
	
	public static JSONObject keepSendingUntilSuccess(String filename, String data, String action) {

		data = data == null ? "" : data;
		
		if(Main.isInOnlineMode() && (isf_email == null || isf_password == null))
			askForAccountData();
			
		long nonce = getTimeStamp();
		int timeOutInSeconds = 5;

		final String targetUrl = SPAM_FUND_API_URL + filename;

		do {
			String jsonString = request(targetUrl+".php", buildAuthString(nonce)+(data.length() > 0 ? "&"+data : ""));
			String error;
			int errorId = -1;
			if(jsonString == null) {
			    error = determineConnectionError(filename, targetUrl);
            } else try {
				JSONObject obj = new JSONObject(jsonString);
				if(obj.getBoolean("success")) return obj;
				error = obj.getString("error");
				if(obj.has("error_id")) errorId = obj.getInt("error_id");
				nonce = obj.getLong("nonce")+timeOutInSeconds;
			} catch(JSONException e) {
				error = String.format(R.STR.getString("api_broken"), R.URL.getString("spam_fund_email"), e.getMessage());
				uim.logDbg(String.format(R.STR.getString("api_failed_invalid_json"), jsonString));
				nonce = getTimeStamp()+timeOutInSeconds;
			}
			if(!filename.equals("signin") || errorId == -1) {
				uim.logWrn(String.format(R.STR.getString("api_failed_retry"), action, error, timeOutInSeconds));
				sleep(timeOutInSeconds * 1000);
				timeOutInSeconds = Math.min(timeOutInSeconds*2, 120);
			} else {
				uim.logWrn(String.format(R.STR.getString("api_failed_account"), action, error));
				isf_email = null;
				askForAccountData();
				timeOutInSeconds = 5;
			}
		} while (true);
	}

	private  static String buildAuthString(long nonce) {
        if(!Main.isInOnlineMode())
            return "&build="+Main.getBuild();
        String hash = md5((isf_password+"-"+nonce).getBytes());
        return "email="+isf_email.replace("+", "%2B")+"&nonce="+nonce+"&hash="+hash+"&build="+Main.getBuild();
    }

	private static String determineConnectionError(final String filename, final String targetUrl) {
        if(filename.equals("signin")) {
            if(APIManager.request(CMC_API_IOTA, null) == null)
                return String.format(R.STR.getString("api_connection_failed_everything"), targetUrl, CMC_API_IOTA);
            return String.format(R.STR.getString("api_connection_failed_only_spamfund"), targetUrl, CMC_API_IOTA, R.URL.getString("spam_fund_email"));
        }
        return String.format(R.STR.getString("api_connection_failed"), targetUrl);
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

    public static void askForAccountData() {

	    //uim.print(UIManager.ANSI_BOLD+R.STR.getString("config_headline_account")+UIManager.ANSI_RESET + "\n" + R.STR.getString("config_signup"));

        if(isf_email == null || isf_email.length() == 0)
            isf_email = uim.askQuestion(UIQuestion.Q_EMAIL);
        isf_password = uim.askQuestion(UIQuestion.Q_PASSWORD);
        uim.print("");

        APIManager.keepSendingUntilSuccess("signin", null, R.STR.getString("api_action_signing_in"));
    }
	
	public static String[] downloadRemoteNodeLists() {
		JSONArray arr = null;
		int tries = 3;
		
		while(arr == null && tries-- > 0) {
			try {
				arr = new JSONArray(request(THIRD_PARTY_NODE_LIST, ""));
			} catch(Throwable t) {
				if(tries == 0) {
					uim.logWrn(String.format(R.STR.getString("api_download_node_list_failed"), THIRD_PARTY_NODE_LIST));
					uim.logException(t, false);
				} else {
					sleep(5000);
				}
				arr = null;
			}
		}
		
		String[] nodes = new String[arr.length()];
		for(int i = 0; i < nodes.length; i++)
			nodes[i] = arr.getString(i);
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
	
	private static void sleep(int ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }
}