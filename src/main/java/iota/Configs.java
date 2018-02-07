package iota;

import iota.ui.UIManager;
import iota.ui.UIQuestion;

public class Configs {
	
	private static final UIManager uim = new UIManager("Configs");
	
	public static String
			isf_email = null,
			isf_password = null,
			time_format = "HH:mm:ss",
			nodes = "";
	public static int log_interval = 20,
			spam_threads = 1,
			sync_check_interval = 600;
	public static boolean import_node_list = false;
	
	public static void init() {
		if(FileManager.exists("config.txt"))
			loadConfigs();
		else
			generateConfigs();
	}
	
	private static void generateConfigs() {
		uim.logInf("=== CONFIGURATION ===");
		uim.logInf("no config.txt file could be found, therefore one will be created now");
		uim.print("");
		
		askForNodeAddress();
		askForThreads();
		askForAccountData(true);
		
		saveConfigs();
	}
	
	private static void saveConfigs() {

		String s = FileManager.readFileFromResource("config_template.txt");
		s = s.replace("%node_list%", nodes);
		s = s.replace("%spam_threads%", ""+spam_threads);
		s = s.replace("%import_node_list%", ""+(import_node_list ? "TRUE" : "FALSE"));
		
		if(isf_email != null) {
			s = s.replace("#ISF_EMAIL", "ISF_EMAIL");
			s = s.replace("%isf_email%", ""+isf_email);
			
			if(isf_password != null) {
				s = s.replace("#ISF_PASSWORD", "ISF_PASSWORD");
				s = s.replace("%isf_password%", ""+isf_password);
			}
		}
		
		byte[] buffer = s.getBytes();
		FileManager.write("config.txt", buffer);
		
		uim.print("thank you. configuration is complete. all your settings were saved successfully");
	}
	
	public static void askForAccountData(boolean settingUp) {
		if(settingUp) {
			uim.print(UIManager.ANSI_BOLD+"[3/3] What is your ISF signin data?\n");
			uim.print("If you haven't signed up yet, visit http://iotaspam.com/account/?p=signup to do so.");
		}
		
		if((!settingUp && isf_email == null) || (settingUp && uim.askForBoolean("do you want your email address written into your config.txt so you don't have to type it in everytime you start this program?")))
			isf_email = uim.askQuestion(UIQuestion.Q_EMAIL);
		if(!settingUp || (settingUp && isf_email != null && uim.askForBoolean("do you want your password written in plain text into your config.txt so you don't have to type it in everytime you start this program?")))
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
		import_node_list = uim.askForBoolean("[1/3] Do you want to use the community node list from 'www.iotanode.host'? ('no' if you ONLY want to use your own node list)");
		
		if(!import_node_list || uim.askForBoolean("[1/3] Do you have other nodes which you want to add to your node list?")) {
			uim.print(UIManager.ANSI_BOLD+"[1/3] Which nodes do you want to connect to?");
			uim.print("");
			uim.print("Format: 'protocol://host:port', e.g. 'http://node.example.org:14265'. You can add multiple nodes by seperating them with commas (',').");
			
			do {
				String nodeInput = uim.askQuestion(UIQuestion.Q_NODES);
				NodeManager.addNode(nodeInput);
			} while(uim.askForBoolean("do you want to add another node?"));
			nodes = NodeManager.getNodeListString();
		}
		
		if(import_node_list) NodeManager.importRemoteNodeList();
	}
	
	private static void loadConfigs() {
		uim.logInf("=== INITIALIZE CONFIGURATIONS ===");
		String configString = FileManager.readFile("config.txt").replaceAll("\t", "").replaceAll(" ", "");
		
		String[] configs = configString.replaceAll("(\\\\_)", " ").split("\n");
		for(String cfg : configs) {
			cfg = cfg.split("#")[0];
			if(cfg.length() == 0) continue;
			
			String par = cfg.split(":")[0].toUpperCase();
			String val = "";
			for(int i = 1; i < cfg.split(":").length; i++) val += cfg.split(":")[i] + (i < cfg.split(":").length-1 ? ":" : "");
			
			if(!par.equals("ISF_PASS") && !par.equals("ISF_PASSWORD") && !par.equals("PASS") && !par.equals("PASSWORD"))
				uim.logDbg("initialize parameter '"+par+"' with value '"+val+"'");
			
			if(par.equals("NODE_LIST") || par.equals("NODES")) { nodes = val; NodeManager.addToNodeList(val); }
			else if(par.equals("LOG_INTERVAL")) { log_interval = Integer.parseInt(val); }
			else if(par.equals("SYNC_CHECK_INTERVAL")) { sync_check_interval = Integer.parseInt(val); }
			else if(par.equals("SPAM_THREADS") || par.equals("THREADS")) { spam_threads = Integer.parseInt(val); }
			else if(par.equals("ISF_EMAIL") || par.equals("EMAIL")) { isf_email = val; }
			else if(par.equals("ISF_PASS") || par.equals("ISF_PASSWORD") || par.equals("PASS") || par.equals("PASSWORD")) { isf_password = val; }
			else if(par.equals("TIME_FORMAT")) { time_format = val; }
			else if(par.equals("IMPORT_NODE_LIST")) {
				val = val.toLowerCase();
				import_node_list = val.equals("true");
				if(!val.equals("true") && !val.equals("false"))
					uim.logWrn("parameter 'import_node_list' can only be set to 'true' or 'false'. Please recheck your config.txt file");
				if(import_node_list) NodeManager.importRemoteNodeList();
			}
			else { uim.logWrn("there is no such parameter: '"+par+"', please recheck your config.txt file"); }
		}
		
		if(isf_email == null || isf_password == null) {
			askForAccountData(false);
		}
		uim.logDbg("signing in using account: '"+isf_email+"'");
		SpamFundAPI.keepSendingUntilSuccess("signin", null, "signing in");
	}
}
