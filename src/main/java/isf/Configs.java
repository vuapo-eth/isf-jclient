package isf;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.ini4j.Wini;

import isf.ui.UIManager;
import isf.ui.UIQuestion;
import isf.ui.UIQuestionInt;

public class Configs {
	
	private static final UIManager UIM = new UIManager("Configs");
	private static final String CONFIG_FILE_NAME = "config-"+Main.buildFullVersion()+".ini";
	
	private static Wini wini;
	
	public static String
			isf_email = null,
			isf_password = null;
	
	public static void loadOrGenerate() {
		if(FileManager.exists(CONFIG_FILE_NAME)) loadWini();
		else generate();
	}
	
	private static void generate() {
		UIM.print("=== CONFIGURATION ===");
		UIM.print("no "+CONFIG_FILE_NAME+" file could be found, therefore one will be created now");
		UIM.print("");
		
		loadWini();
		askForNodes(true);
		askForAccountData(true);
		askForColors();
		saveWini();
	}
	
	private static void askForColors() {
		UIManager.toggleColors(true);
		set(P.LOG_COLORS_ENABLED, UIM.askForBoolean(UIManager.ANSI_BLUE+"Does this text appear blue?"));
		UIManager.toggleColors(getBln(P.LOG_COLORS_ENABLED));
	}
	
	public static void askForAccountData(boolean settingUp) {
		if(settingUp) {
			UIM.print(UIManager.ANSI_BOLD+"[2/2] Sign in using your IOTA Spam Fund account\n");
			UIM.print("If you haven't signed up yet, visit http://iotaspam.com/account/?p=signup to do so.");
		}
		
		if(isf_email == null || isf_email.length() == 0)
			isf_email = UIM.askQuestion(UIQuestion.Q_EMAIL);
		isf_password = UIM.askQuestion(UIQuestion.Q_PASSWORD);
		
		if(settingUp) {
			APIManager.keepSendingUntilSuccess("signin", null, "signing in");
			
			int saveMode = UIM.askQuestionInt(UIQuestionInt.Q_SAVE_ACCOUNT_DATA);
			set(P.SPAMFUND_EMAIL, saveMode > 0 ? isf_email : "");
			set(P.SPAMFUND_PASSWORD, saveMode == 2 ? isf_password : "");
			saveWini();
		}
	}
	
	private static void askForNodes(boolean firstSetup) {
		
		NodeManager.clearNodes();
		
		set(P.NODES_THIRD_PARTY_LIST, UIM.askForBoolean((firstSetup ? "[1/2] " : "")+"do you want to use third party node lists?"));
		
		if(!getBln(P.NODES_THIRD_PARTY_LIST) || UIM.askForBoolean("do you want to add other nodes to your node list?")) {
			String nodeInput = null;
			do {
				nodeInput = UIM.askQuestion(UIQuestion.Q_NODES);
				if(nodeInput.length() > 0) {
					NodeManager.addNode(nodeInput, false);
					set(P.NODES_LIST, NodeManager.buildNodeListString());
				}
			} while(nodeInput.length() > 0 && UIM.askForBoolean("do you want to add another node?"));
		}
		
		if(getBln(P.NODES_THIRD_PARTY_LIST))
			NodeManager.importRemoteNodeList();

		int recommended = Math.max(1, Math.min(10, NodeManager.getAmountOfNodes()));
		UIQuestionInt.Q_NODES_AMOUNT_ROTATION.setMax(NodeManager.getAmountOfNodes()).setRecommended(recommended);
		set(P.NODES_AMOUNT_ROTATION, firstSetup ? recommended : UIM.askQuestionInt(UIQuestionInt.Q_NODES_AMOUNT_ROTATION));
	}
	
	private static void load() {
		UIManager.toggleColors(getBln(P.LOG_COLORS_ENABLED));
		
		UIM.logDbg("loading configurations");
		
		isf_email = wini.get("spamfund", "email");
		if(isf_email.length() == 0) isf_email = null;
		isf_password = wini.get("spamfund", "password");
		if(isf_password.length() == 0) isf_password = null;

		if(isf_email == "" || isf_password == "")
			askForAccountData(false);
		
		UIM.logDbg("signing in using account: '"+isf_email+"'");
		APIManager.keepSendingUntilSuccess("signin", null, "signing in");
		
		UIM.logDbg("configurations loaded successfully");
	}
	
	public static void edit() {

		UIM.logDbg("editing configurations");
		
		String variable = "";
		while(!variable.equals("save")) {
			variable = UIM.askQuestion(new UIQuestion() {
				
				@Override
				public boolean isAnswer(String str) {
					return str.equals("account") || str.equals("nodes") || str.equals("threads") || str.equals("log") || str.equals("save");
				}
				
			}.setQuestion("what parameter do you want to change? [account/nodes/threads/log | SAVE]"));

			if(variable.equals("threads")) {
				int amountOfCores = Runtime.getRuntime().availableProcessors();
				UIQuestionInt.Q_THREADS_AMOUNT_POW.setRange(1, amountOfCores);
				UIQuestionInt.Q_THREADS_AMOUNT_POW.setQuestion("how many threads do you want to use for performing Proof-of-Work? (your processor has "+amountOfCores+" cores) ")
					.setRecommended(Math.max(1, amountOfCores-1));
				set(P.THREADS_AMOUNT_POW, UIM.askQuestionInt(UIQuestionInt.Q_THREADS_AMOUNT_POW));
				set(P.THREADS_PRIORITY_POW, UIM.askQuestionInt(UIQuestionInt.Q_THREADS_PRIORITY_POW));
				set(P.THREADS_TIP_POOL_SIZE, UIM.askQuestionInt(UIQuestionInt.Q_THREADS_GTTARS_SIZE));
			} else if(variable.equals("account")) {
				isf_email = null;
				isf_password = null;
				APIManager.keepSendingUntilSuccess("signin", null, "signing in");
				set(P.SPAMFUND_EMAIL, isf_email == null ? "" : isf_email);
				set(P.SPAMFUND_PASSWORD, isf_password == null ? "" : isf_password);
			} else if(variable.equals("nodes")) {
				askForNodes(false);
			} else if(variable.equals("log")) {
				Configs.set(P.LOG_INTERVAL, UIM.askQuestionInt(UIQuestionInt.Q_LOG_INTERVAL));
				do Configs.set(P.LOG_TIME_FORMAT, UIM.askQuestion(UIQuestion.Q_TIME_FORMAT));
				while(!UIM.askForBoolean("do you really want your time to be displayed like that: " + new SimpleDateFormat(get(P.LOG_TIME_FORMAT)).format(new Date()) +"?"));
				askForColors();
			}
		}
		
		saveWini();
		
	}
	
	private static void saveWini() {
		try {
			wini.store();
			UIM.logDbg("configurations saved successfully");
		} catch (IOException e) {
			UIM.logWrn("saving configurations failed");
			UIM.logException(e, false);
		}
	}
	
	private static void initWini() {
		int amountOfCores = Runtime.getRuntime().availableProcessors();
		
		UIM.logDbg("generating default configuration");

		set(P.GENERAL_VERSION, Main.buildFullVersion());
		set(P.NODES_SYNC_CHECK_INTERVAL, 600);
		set(P.NODES_AMOUNT_ROTATION, 20);
		set(P.NODES_LIST, "");
		set(P.NODES_THIRD_PARTY_LIST, true);
		set(P.LOG_COLORS_ENABLED, true);
		set(P.LOG_INTERVAL, 60);
		set(P.LOG_TIME_FORMAT, "HH:mm:ss");
		set(P.THREADS_TIP_POOL_SIZE, 10);
		set(P.THREADS_AMOUNT_POW, Math.max(1, amountOfCores-1));
		set(P.THREADS_PRIORITY_POW, 3);
		set(P.SPAMFUND_EMAIL, "");
		set(P.SPAMFUND_PASSWORD, "");
	}
	
	public static int getInt(P p) {
		return wini.get(p.parent, p.name, int.class);
	}
	
	public static boolean getBln(P p) {
		return wini.get(p.parent, p.name, boolean.class);
	}
	
	public static String get(P p) {
		return wini == null ? null : wini.get(p.parent, p.name);
	}
	
	private static void set(P p, Object o) {
		wini.put(p.parent, p.name, o);
	}
	
	private static void loadWini() {
		File f = FileManager.getFile(CONFIG_FILE_NAME);
		
		try {
			if(!f.exists()) {
				f.createNewFile();
				wini = new Wini(f);
				initWini();
				saveWini();
			} else {
				wini = new Wini(f);
				load();
			}
			
			NodeManager.addToNodeList(get(P.NODES_LIST).replace(" ", ""));
			if(getBln(P.NODES_THIRD_PARTY_LIST)) NodeManager.importRemoteNodeList();
		} catch (IOException e) {
			UIM.logWrn("loading configuration file failed [maybe the jar is missing permission to write here, try to start it with 'sudo java -jar ...']");
			UIM.logException(e, true);
		}
	}
}
