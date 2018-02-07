package iota;

import java.io.File;
import java.io.IOException;

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
			spam_threads = 1,
			sync_check_interval = 600;
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
		askForThreads(true);
		askForAccountData(true);
	}
	
	public static void askForAccountData(boolean settingUp) {
		if(settingUp) {
			uim.print(UIManager.ANSI_BOLD+"[3/3] Sign in using your ISF account\n");
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
	
	private static void askForThreads(boolean firstSetup) {
		if(firstSetup)
			uim.print(UIManager.ANSI_BOLD+"[2/3] How many spamming threads do you want to run?\n");
		
		uim.print(UIManager.ANSI_BRIGHT_BLACK+UIManager.ANSI_BOLD+">>> WHAT IS A THREAD?\n"+UIManager.ANSI_RESET
				+UIManager.ANSI_BRIGHT_BLACK+"Threads are processes doing calculations isolated from each other. This means you can run\n"
											+"multiple threads parallelly for better performance.");
		uim.print(UIManager.ANSI_BRIGHT_BLACK+UIManager.ANSI_BOLD+">>> HOW MANY SHOULD I USE?\n"+UIManager.ANSI_RESET
				+UIManager.ANSI_BRIGHT_BLACK+"Depending on your processor, you have a certain amount of cores. For maximum performance use\n"
										    +"as many threads as you have cores. "+UIManager.ANSI_RESET+UIManager.ANSI_BOLD+"You have "+Runtime.getRuntime().availableProcessors()+" cores.");
		
		spam_threads = uim.askForInteger("how many threads do you want to use for spamming?", 1, Runtime.getRuntime().availableProcessors());
	}
	
	private static void askForNodes(boolean firstSetup) {
		third_party_node_list = uim.askForBoolean((firstSetup ? "[1/3]" : "")+" do you want to use the community node list from 'www.iotanode.host'?");
		
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
		isf_password = wini.get("spamfund", "password");
		
		spam_threads = wini.get("other", "threads", int.class);

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
					return str.equals("account") || str.equals("nodes") || str.equals("threads") || /*str.equals("log") ||*/ str.equals("save");
				}
				
			}.setQuestion("what parameter do you want to change? [account/nodes/threads"/*"/log"*/+" | SAVE]")); // TODO

			if(variable.equals("threads")) {
				askForThreads(false);
				wini.put("other", "threads", spam_threads);
			}

			if(variable.equals("account")) {
				isf_email = null;
				isf_password = null;
				SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
				wini.put("spamfund", "email", isf_email == null ? "" : isf_email);
				wini.put("spamfund", "password", isf_password == null ? "" : isf_password);
			}

			if(variable.equals("nodes")) {
				nodes = "";
				askForNodes(false);
				wini.put("nodes", "node_list", nodes);
				wini.put("nodes", "third_party_node_list", third_party_node_list);
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
		
		wini.put("other", "threads", spam_threads);
		
		try {
			wini.store();
			uim.logDbg("configurations saved successfully");
		} catch (IOException e) {
			uim.logWrn("saving configurations failed");
			uim.logException(e, false);
		}
	}
	
	private static Wini loadWini(String action) {
		File f = new File("config.ini");
		
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
