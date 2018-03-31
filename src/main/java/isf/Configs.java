package isf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import isf.spam.NodeManager;
import isf.ui.R;
import org.apache.commons.lang3.StringUtils;
import org.ini4j.Wini;

import isf.ui.UIManager;
import isf.ui.UIQuestion;
import isf.ui.UIQuestionInt;

public class Configs {
	
	private static final UIManager UIM = new UIManager("Configs");
	private static final String CONFIG_FILE_NAME = R.URL.getString("config_file_name");
	
	private static Wini wini;
	
	public static void loadOrGenerate() {
		if(FileManager.exists(CONFIG_FILE_NAME)) loadWini();
		else generate();
		UIManager.updateDebugEnabled();
	}
	
	private static void generate() {
		UIM.print(String.format(R.STR.getString("config_create"), CONFIG_FILE_NAME)+"\n");
		loadWini();
		P.LOG_COLORS_ENABLED.edit();
		askForNodes(true);
		saveWini();
	}
	
	private static void askForNodes(boolean firstSetup) {
		
		NodeManager.clearNodes();
		
		set(P.NODES_THIRD_PARTY_NODE_LIST, UIM.askForBoolean(R.STR.getString("config_question_third_party_node_list")));
		
		if(!getBln(P.NODES_THIRD_PARTY_NODE_LIST)) {
			String nodeInput = null;
			do {
				nodeInput = UIM.askQuestion(UIQuestion.Q_NODES);
				if(nodeInput.length() > 0) {
					NodeManager.addNode(nodeInput, false);
					FileManager.write("nodelist.cfg", NodeManager.buildNodesFileHeader(false) + NodeManager.buildNodeListString());
					FileManager.write("nodelist_testnet.cfg", NodeManager.buildNodesFileHeader(true));
				}
			} while(nodeInput.length() > 0 && UIM.askForBoolean(R.STR.getString("config_question_add_node")));
		}

		NodeManager.clearNodes();
		NodeManager.loadNodeList();

		if(!firstSetup) {
			int recommended = Math.max(1, Math.min(Integer.parseInt(P.NODES_AMOUNT_ROTATION.defaultValue), NodeManager.getAmountOfNodes()));
			UIQuestionInt.Q_NODES_AMOUNT_ROTATION.setMax(NodeManager.getAmountOfNodes()).setRecommended(recommended);
			set(P.NODES_AMOUNT_ROTATION, UIM.askQuestionInt(UIQuestionInt.Q_NODES_AMOUNT_ROTATION));
		}
	}
	
	private static void load() {
		UIM.logDbg(R.STR.getString("config_loading"));

		for(P p : P.values())
			set(p, get(p));
		saveWini();
		
		UIManager.toggleColors(getBln(P.LOG_COLORS_ENABLED));

        // correct spam address if not correct
        final Pattern VALID_TRYTES = Pattern.compile("^[A-Z9]*$");
        String spamAddress = get(P.SPAM_OFFLINE_ADDRESS).toUpperCase();
        final Pattern VALID_SPAM_ADDRESS = Pattern.compile("^[A-Z9]{81}$");
        if(!VALID_SPAM_ADDRESS.matcher(spamAddress).find()) {
            if(!VALID_TRYTES.matcher(spamAddress).find())
                spamAddress = "";
            spamAddress = spamAddress.substring(0, Math.min(spamAddress.length(), 81));
            spamAddress = spamAddress + StringUtils.repeat("9",81-spamAddress.length());
            set(P.SPAM_OFFLINE_ADDRESS, spamAddress);
        }

        // correct spam tag if not correct
        String spamTag = get(P.SPAM_OFFLINE_TAG).toUpperCase();
        final Pattern VALID_SPAM_TAG = Pattern.compile("^[A-Z9]{27}$");
        if(!VALID_SPAM_ADDRESS.matcher(spamTag).find()) {
            if(!VALID_TRYTES.matcher(spamTag).find())
                spamTag = "";
            spamTag = spamTag.substring(0, Math.min(spamTag.length(), 27));
            spamTag = spamTag + StringUtils.repeat("9",27-spamTag.length());
            set(P.SPAM_OFFLINE_TAG, spamTag);
        }

        UIM.logDbg(R.STR.getString("config_loading_success"));
	}
	
	public static void edit() {

        String sectionsString = "";
        final ArrayList<String> sections = new ArrayList<String>();
        for(P p : P.values())
            if(p.isEditable() && !sections.contains(p.parent)) {
                sections.add(p.parent);
                sectionsString += (sectionsString.length() > 0 ? "/" : "") + p.parent;
            }

        final UIQuestion sectionQuestion = new UIQuestion() {
            @Override
            public boolean isAnswer(String str) {
                return sections.contains(str) || str.equals("save");
            }
        }.setQuestion(R.STR.getString("config_menu_section") + " ["+sectionsString+" | SAVE]");

		UIM.logDbg(R.STR.getString("config_editing"));
		
		String section = "";
		do {
            section = UIM.askQuestion(sectionQuestion);
			if(section.equals("save")) break;

			openPropertyMenu(section);
		} while(true);
		
		saveWini();
	}

	public static void openPropertyMenu(final String section) {

        final ArrayList<P> properties = new ArrayList<P>();

        String parameterString = "";
        for(P p : P.values())
            if(p.isEditable() && p.parent.equals(section)) {
                properties.add(p);
                parameterString += (parameterString.length() > 0 ? ", " : "") + properties.size() + " = " + p.name;
            }

        final UIQuestionInt propertyQuestion = new UIQuestionInt(0, properties.size(), false).setQuestion(R.STR.getString("config_menu_parameter") + " ["+parameterString+" | 0 = BACK]");

        do {
            int property = UIM.askQuestionInt(propertyQuestion);
            if(property == 0) break;
            P p = properties.get(property-1);
            String strResource = get(p).equals(p.defaultValue) ? "config_parameter_value_is_default" : "config_parameter_value";
            UIM.print("\n"+String.format(R.STR.getString(strResource), p.name, get(p), p.defaultValue));
            properties.get(property-1).edit();
        } while(true);
    }
	
	private static void saveWini() {
		try {
			wini.store();
			UIM.logDbg(R.STR.getString("config_saving_success"));
		} catch (IOException e) {
			UIM.logWrn(R.STR.getString("config_saving_fail"));
			UIM.logException(e, false);
		}
	}
	
	private static void initWini() {
		
		UIM.logDbg(R.STR.getString("config_generating_default"));

		for(P p : P.values())
			set(p, p.defaultValue);
	}
	
	public static int getInt(P p) {
		return Integer.parseInt(get(p));
	}
	
	public static boolean getBln(P p) {
		return Boolean.parseBoolean(get(p));
	}
	
	public static String get(P p) {
		return (String)p.get(wini);
	}
	
	public static void set(P p, Object o) {
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
		} catch (IOException e) {
			UIM.logWrn(R.STR.getString("config_loading_fail"));
			UIM.logException(e, true);
		}
	}
}
