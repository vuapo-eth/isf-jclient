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
	public static boolean import_node_list = false;
	
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
		
		askForNodeAddress();
		askForThreads();
		askForAccountData(true);
		
		saveConfigs();
	}
	
	public static void askForAccountData(boolean settingUp) {
		if(settingUp) {
			uim.print(UIManager.ANSI_BOLD+"[3/3] Sign in using your ISF account\n");
			uim.print("If you haven't signed up yet, visit http://iotaspam.com/account/?p=signup to do so.");
		}
		
		if((!settingUp && isf_email == null) || (settingUp && uim.askForBoolean("do you want your email address written into your config.ini so you don't have to type it in everytime you start this program?")))
			isf_email = uim.askQuestion(UIQuestion.Q_EMAIL);
		if(!settingUp || (isf_email != null && uim.askForBoolean("do you want your password written in plain text into your config.ini so you don't have to type it in everytime you start this program?")))
			isf_password = uim.askQuestion(UIQuestion.Q_PASSWORD);
		
		SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
	}
	
	private static void askForThreads() {
		uim.print(UIManager.ANSI_BOLD+"[2/3] How many spamming threads do you want to run?");
		uim.print("");
		uim.print(UIManager.ANSI_BRIGHT_BLACK+UIManager.ANSI_BOLD+">>> WHAT IS A THREAD?\n"+UIManager.ANSI_RESET
				+UIManager.ANSI_BRIGHT_BLACK+"Threads are processes doing calculations isolated from each other. This means you can run\n"
											+"multiple threads parallelly for better performance.");
		uim.print(UIManager.ANSI_BRIGHT_BLACK+UIManager.ANSI_BOLD+">>> HOW MANY SHOULD I USE?\n"+UIManager.ANSI_RESET
				+UIManager.ANSI_BRIGHT_BLACK+"Depending on your processor, you have a certain amount of cores. For maximum performance use\n"
										    +"as many threads as you have cores. "+UIManager.ANSI_RESET+UIManager.ANSI_BOLD+"You have "+Runtime.getRuntime().availableProcessors()+" cores.");
		
		spam_threads = uim.askForInteger("how many threads do you want to use for spamming?", 1, Runtime.getRuntime().availableProcessors());
	}
	
	private static void askForNodeAddress() {
		import_node_list = uim.askForBoolean("[1/3] Do you want to use the community node list from 'www.iotanode.host'?");
		
		if(!import_node_list || uim.askForBoolean("do you want to add other nodes to your node list?")) {
			
			do {
				String nodeInput = uim.askQuestion(UIQuestion.Q_NODES);
				NodeManager.addNode(nodeInput);
			} while(uim.askForBoolean("do you want to add another node?"));
			nodes = NodeManager.getNodeListString();
		}
		
		if(import_node_list) NodeManager.importRemoteNodeList();
	}
	
	private static void loadConfigs() {
		uim.logDbg("loading configurations");
		
		Wini wini;
		
		try {
			wini = new Wini(new File("config.ini"));
		} catch (IOException e) {
			uim.logWrn("loading configurations failed");
			uim.logException(e, true);
			return;
		}

		sync_check_interval = wini.get("nodes", "sync_check_interval", int.class);
		nodes = wini.get("nodes", "node_list");
		NodeManager.addToNodeList(nodes);
		
		import_node_list = wini.get("nodes", "import_from_third_party", boolean.class);
		if(import_node_list) NodeManager.importRemoteNodeList();

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
	
	private static void saveConfigs() {

		File f = new File("config.ini");
		
		uim.logDbg("saving configurations");
		
		Wini wini;
		
		try {
			if(!f.exists()) f.createNewFile();
			wini = new Wini(f);
		} catch (IOException e) {
			uim.logWrn("saving configurations failed");
			uim.logException(e, false);
			return;
		}

		wini.put("nodes", "sync_check_interval", sync_check_interval);
		wini.put("nodes", "node_list", nodes);
		
		wini.put("nodes", "import_from_third_party", import_node_list);

		wini.put("log", "interval", log_interval);
		wini.put("log", "time_format", time_format);
		
		wini.put("spamfund", "email", isf_email == null ? "" : isf_email);
		wini.put("spamfund", "password", isf_password == null ? "" : isf_password);
		
		wini.put("other", "threads", spam_threads);
		
		uim.logDbg("configurations saved successfully");
	}
}
