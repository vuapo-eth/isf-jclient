package iota;

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

import iota.ui.UIManager;

public class SpamFundAPI {
	
	private static final UIManager uim = new UIManager("ISF-API");

	private static final String SPAM_FUND_API_URL = "http://mikrohash.de/isf/api/"+Main.getVersion()+"/";
	private static final String NODE_LIST_JSON = "https://iotanode.host/node_table.json";
	private static final String CMC_API_IOTA = "https://api.coinmarketcap.com/v1/ticker/iota/";

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
			uim.logException(e, false);
		}
		return "";
	}
	
	public static String requestUpdates() {
		return request(SPAM_FUND_API_URL + "updates.php", "");
	}
	
	public static int requestCommand() {
		return keepSendingUntilSuccess("command", "", "requesting remote command").getInt("command");
	}

	public static String requestSpamAddress() {
		return keepSendingUntilSuccess("address", "", "requesting spam address").getString("address");
	}

	public static void printRewards() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		JSONObject objAnswer = keepSendingUntilSuccess("rewards", "", "requesting reward stats");
		JSONArray tails = objAnswer.getJSONArray("tails");
		uim.logPln("Here is a list of all your rewards:");
		int sumConfirmed = 0, sumBalance = 0;
		for(int i = 0; i < tails.length(); i++) {
			JSONObject obj = tails.getJSONObject(i);
			sumConfirmed += obj.getInt("confirmed");
			sumBalance += obj.getInt("balance");
			uim.print(UIManager.padLeft(obj.getInt("balance")+"", 8) + "i |" + UIManager.padLeft(obj.getInt("confirmed") + "", 5) + " conf. txs | " + sdf.format(obj.getLong("created")*1000) + " | " + obj.getString("trytes"));
		}
		uim.print("==========|================|===============================================================");
		uim.print(UIManager.padLeft(sumBalance+"", 8) + "i |" + UIManager.padLeft(sumConfirmed + "", 15) + " | total rewards as of now");
	}
	
	public static void saveTail(Tail tail) {
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
	
	public static double getIotaPrice() {
		String jsonString = request(CMC_API_IOTA, null);
		DecimalFormat df = new DecimalFormat("###,##0.00");
		DecimalFormat df2 = new DecimalFormat("###,##0");
		try {
			JSONObject obj = new JSONArray(jsonString).getJSONObject(0);
			uim.logInf(UIManager.ANSI_BOLD+"IOTA TICKER:      " + UIManager.ANSI_RESET + "PRICE (USD): $" + df.format(obj.getDouble("price_usd")) + "/Mi "
					+ "("+(obj.getDouble("percent_change_24h")<0?UIManager.ANSI_RED:UIManager.ANSI_GREEN+"+")
					+ df.format(obj.getDouble("percent_change_24h"))+"%"+UIManager.ANSI_RESET+" in 24h)" + " | "
					+ "PRICE (BTC): " + df2.format((int) (100000000*obj.getDouble("price_btc"))) + " sat/Mi | "
					+ "MCAP: $"+df.format(obj.getDouble("market_cap_usd")/1e9)+"B (#"+obj.getInt("rank")+")");
			return obj.getDouble("price_usd");
		} catch (JSONException e) {
			uim.logDbg(jsonString);
			uim.logException(e, false);
			return 0;
		}
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
	
	public static String[] requestNodes() {
		JSONArray arr = new JSONArray(request(NODE_LIST_JSON, ""));
		String[] nodes = new String[arr.length()];
		for(int i = 0; i < arr.length(); i++)
			nodes[i] = arr.getJSONObject(i).getString("host");
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