package isf;

import isf.ui.R;
import isf.ui.UIManager;
import isf.ui.UIQuestion;
import isf.ui.UIQuestionInt;
import org.apache.commons.lang3.StringUtils;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import java.text.SimpleDateFormat;
import java.util.Date;

public enum P {
	
	GENERAL_VERSION(Main.buildFullVersion()),

    SPAM_DEPTH(UIQuestionInt.Q_SPAM_DEPTH),
    SPAM_WIKIPEDIA_MESSAGE(true, R.STR.getString("config_question_wikipedia_message")),
    SPAM_OFFLINE_ADDRESS("OFFLINE9SPAM9ADDRESS9999999999999999999999999999999999999999999999999999999999999", new EditHandler() {
        @Override
        void edit() {
            String spamAddress = UIM.askQuestion(UIQuestion.Q_SPAM_ADDRESS).toUpperCase();
            spamAddress = spamAddress.substring(0, Math.min(spamAddress.length(), 81));
            spamAddress = spamAddress + StringUtils.repeat("9",81-spamAddress.length());
            SPAM_OFFLINE_ADDRESS.set(spamAddress);
        }
    }),

    SPAM_OFFLINE_TAG("IOTASPAM9DOT9COM9999OFFLINE", new EditHandler() {
        @Override
        void edit() {
            String spamAddress = UIM.askQuestion(UIQuestion.Q_SPAM_TAG).toUpperCase();
            spamAddress = spamAddress.substring(0, Math.min(spamAddress.length(), 27));
            spamAddress = spamAddress + StringUtils.repeat("9",27-spamAddress.length());
            SPAM_OFFLINE_TAG.set(spamAddress);
        }
    }),

    POW_CORES(UIQuestionInt.Q_THREADS_AMOUNT_POW),
    POW_ABORT_TIME(UIQuestionInt.Q_POW_ABORT_TIME),
    POW_USE_GO_MODULE(true, R.STR.getString("config_question_use_go")),
    LOG_DEBUG_ENABLED("true", new EditHandler() {
        @Override
        void edit() {
            LOG_DEBUG_ENABLED.set(UIM.askForBoolean(R.STR.getString("config_question_debug")));
            UIManager.updateDebugEnabled();
        }
    }),
    LOG_INTERVAL(UIQuestionInt.Q_LOG_INTERVAL),
    LOG_PERFORMANCE_REPORT_INTERVAL(UIQuestionInt.Q_LOG_PERFORMANCE_REPORT_INTERVAL),

	LOG_COLORS_ENABLED("true", new EditHandler() { @Override void edit() {
        UIManager.toggleColors(true);
        LOG_COLORS_ENABLED.set(UIM.askForBoolean(UIManager.ANSI_BLUE+ R.STR.getString("config_question_blue")));
        UIManager.toggleColors(Configs.getBln(P.LOG_COLORS_ENABLED));
    }}),

	LOG_TIME_FORMAT("HH:mm:ss", new EditHandler() {
        @Override
        void edit() {
            do Configs.set(P.LOG_TIME_FORMAT, UIM.askQuestion(UIQuestion.Q_TIME_FORMAT));
            while(!UIM.askForBoolean(String.format(R.STR.getString("config_question_time_okay"), new SimpleDateFormat(Configs.get(P.LOG_TIME_FORMAT)).format(new Date()))));
        }
    }),


	NODES_AMOUNT_ROTATION(UIQuestionInt.Q_NODES_AMOUNT_ROTATION),
	NODES_THIRD_PARTY_NODE_LIST(true, R.STR.getString("config_question_third_party_node_list")),
	NODES_SYNC_CHECK_INTERVAL(UIQuestionInt.Q_NODE_SYNC_CHECK_INTERVAL);

    private static final UIManager UIM = new UIManager("Prprts");
	public final String parent, name;
	public final String defaultValue;
	public EditHandler editHandler;

    P(String defaultValue) {
        String enumName = toString().toLowerCase();
        parent = enumName.split("_")[0];
        name = enumName.substring(parent.length()+1, enumName.length());
        this.defaultValue = defaultValue;
    }

    P(String defaultValue, EditHandler editHandler) {
        this(defaultValue);
        this.editHandler = editHandler;
    }

    P(Boolean defaultValue, final String booleanQuestion) {
        this(defaultValue.toString());
        this.editHandler = new EditHandler() { @Override void edit() { P.this.set(UIM.askForBoolean(booleanQuestion)); }};
    }

    P(final UIQuestionInt uiQuestionInt) {
        this(""+uiQuestionInt.getRecommended());
        this.editHandler = new EditHandler() { @Override void edit() { P.this.set(UIM.askQuestionInt(uiQuestionInt)); }};
    }
	
	public Object get(Wini wini) {
		Section sec = wini.getOrDefault(parent, null);
		if(sec == null) return defaultValue;
		return sec.getOrDefault(name, defaultValue);
	}

	private void set(Object o) {
        Configs.set(this, o);
    }

	public void edit() {
        editHandler.edit();
    }

    public boolean isEditable() {
        return editHandler != null;
    }
}

abstract class EditHandler {
    abstract void edit();
}