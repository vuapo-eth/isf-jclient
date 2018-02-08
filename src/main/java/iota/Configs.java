package iota;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ini4j.Wini;

import iota.ui.UIManager;
import iota.ui.UIQuestion;

public class Configs {
	
	private static final UIManager uim = new UIManager("Configs");
	
	public static String
			isf_email = null,
			isf_password = null,
			time_format = "HH:mm:ss",
			nodes = "";
	public static int log_interval = 60,
			threads_amount = 1,
			sync_check_interval = 600,
			threads_priority = 2;
	public static boolean third_party_node_list = false;
	
	public static void init() {
		if(FileManager.exists("config.ini"))
			loadConfigs();
		else
			generateConfigs();
	}
	
	private static void generateConfigs() {
		uim.logInf("=== CONFIGURATION ===");
		uim.logInf("no config.ini file could be found, therefore one will be created now");
		uim.print("");
		
		askForNodes(true);
		askForAccountData(true);
	}
	
	public static void askForAccountData(boolean settingUp) {
		if(settingUp) {
			uim.print(UIManager.ANSI_BOLD+"[2/2] Sign in using your ISF account\n");
			uim.print("If you haven't signed up yet, visit http://iotaspam.com/account/?p=signup to do so.");
		}
		
		boolean saveEmail = false, savePassword = false;
		
		if((!settingUp && isf_email == null) || (settingUp && (saveEmail = uim.askForBoolean("do you want your email address written into your config.ini so you don't have to type it in everytime you start this program?"))))
			isf_email = uim.askQuestion(UIQuestion.Q_EMAIL);
		if(!settingUp || (isf_email != null && (savePassword = uim.askForBoolean("do you want your password written in plain text into your config.ini so you don't have to type it in everytime you start this program?"))))
			isf_password = uim.askQuestion(UIQuestion.Q_PASSWORD);
		
		
		SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
		
		if(settingUp) {
			String backup_isf_email = isf_email;
			String backup_isf_password = isf_password;
			if(!saveEmail)
				isf_email = "";
			if(!savePassword)
				isf_password = "";
			saveConfigs();
			isf_email = backup_isf_email;
			isf_password = backup_isf_password;
		}
	}
	
	private static void askForThreads() {
		threads_amount = uim.askForInteger("how many threads do you want to use for spamming? (your processor has "+Runtime.getRuntime().availableProcessors()+" cores) ", 1, Runtime.getRuntime().availableProcessors());
		threads_priority = uim.askForInteger("how much computational resources do you want to give each thread (1 = minimum, 2 = normal, 3 = maximum) ", 1, 3);
	}
	
	private static void askForNodes(boolean firstSetup) {
		third_party_node_list = uim.askForBoolean((firstSetup ? "[1/2]" : "")+" do you want to use the community node list from 'www.iotanode.host'?");
		
		if(!third_party_node_list || uim.askForBoolean("do you want to add other nodes to your node list?")) {
			
			do {
				String nodeInput = uim.askQuestion(UIQuestion.Q_NODES);
				NodeManager.addNode(nodeInput);
			} while(uim.askForBoolean("do you want to add another node?"));
			nodes = NodeManager.getNodeListString();
		}
		
		if(third_party_node_list) NodeManager.importRemoteNodeList();
	}
	
	private static void loadConfigs() {
		uim.logDbg("loading configurations");
		Wini wini = loadWini("loading configurations");

		sync_check_interval = wini.get("nodes", "sync_check_interval", int.class);
		nodes = wini.get("nodes", "node_list");
		NodeManager.addToNodeList(nodes);
		
		third_party_node_list = wini.get("nodes", "third_party_node_list", boolean.class);
		if(third_party_node_list) NodeManager.importRemoteNodeList();

		log_interval = wini.get("log", "interval", int.class);
		time_format = wini.get("log", "time_format");
		
		isf_email = wini.get("spamfund", "email");
		if(isf_email.length() == 0) isf_email = null;
		isf_password = wini.get("spamfund", "password");
		if(isf_password.length() == 0) isf_password = null;
		
		threads_amount = wini.get("threads", "amount", int.class);
		threads_priority = wini.get("threads", "priority", int.class);

		if(isf_email == "" || isf_password == "") {
			askForAccountData(false);
		}
		
		uim.logDbg("signing in using account: '"+isf_email+"'");
		SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
		
		uim.logDbg("configurations loaded successfully");
	}
	
	public static void editConfigs() {

		uim.logDbg("editing configurations");
		Wini wini = loadWini("editing configurations");
		
		String variable = "";
		while(!variable.equals("save")) {
			variable = uim.askQuestion(new UIQuestion() {
				
				@Override
				public boolean isAnswer(String str) {
					return str.equals("account") || str.equals("nodes") || str.equals("threads") || str.equals("log") || str.equals("save");
				}
				
			}.setQuestion("what parameter do you want to change? [account/nodes/threads/log | SAVE]")); // TODO

			if(variable.equals("threads")) {
				askForThreads();
				wini.put("threads", "amount", threads_amount);
				wini.put("threads", "priority", threads_priority);
			} else if(variable.equals("account")) {
				isf_email = null;
				isf_password = null;
				SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
				wini.put("spamfund", "email", isf_email == null ? "" : isf_email);
				wini.put("spamfund", "password", isf_password == null ? "" : isf_password);
			} else if(variable.equals("nodes")) {
				nodes = "";
				askForNodes(false);
				wini.put("nodes", "node_list", nodes);
				wini.put("nodes", "third_party_node_list", third_party_node_list);
			} else if(variable.equals("log")) {
				log_interval = uim.askForInteger("how many seconds do you wish the logger to wait between each info log (we recommended 60)", 1, 3600);
				wini.put("log", "interval", log_interval);
				do
					time_format = uim.askQuestion(UIQuestion.Q_TIME_FORMAT);
				while(!uim.askForBoolean("do you really want your time to be displayed like that: " + new SimpleDateFormat(time_format).format(new Date()) +"?"));
				wini.put("log", "time_format", time_format);
			}
		}
		
		try {
			wini.store();
			uim.logDbg("configurations edited successfully");
		} catch (IOException e) {
			uim.logWrn("editing configurations failed");
			uim.logException(e, false);
		}
		
	}
	
	private static void saveConfigs() {

		uim.logDbg("saving configurations");
		Wini wini = loadWini("saving configurations");

		wini.put("nodes", "sync_check_interval", sync_check_interval);
		wini.put("nodes", "node_list", nodes);
		wini.put("nodes", "third_party_node_list", third_party_node_list);

		wini.put("log", "interval", log_interval);
		wini.put("log", "time_format", time_format);
		
		wini.put("spamfund", "email", isf_email == null ? "" : isf_email);
		wini.put("spamfund", "password", isf_password == null ? "" : isf_password);

		wini.put("threads", "amount", threads_amount);
		wini.put("threads", "priority", threads_priority);
		
		try {
			wini.store();
			uim.logDbg("configurations saved successfully");
		} catch (IOException e) {
			uim.logWrn("saving configurations failed");
			uim.logException(e, false);
		}
	}
	
	private static Wini loadWini(String action) {
		File f = FileManager.getFile("config.ini");
		
		try {
			if(!f.exists()) f.createNewFile();
			return new Wini(f);
		} catch (IOException e) {
			uim.logWrn(action + " failed");
			uim.logException(e, false);
			return null;
		}
	}
}
