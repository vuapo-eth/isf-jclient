package isf.ui;

import java.util.regex.Pattern;

import isf.Configs;
import isf.P;

public abstract class UIQuestion {

	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.-]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	private static final Pattern VALID_TRYTES = Pattern.compile("^[A-Z9]*$", Pattern.CASE_INSENSITIVE);
	
	public static final UIQuestion Q_PASSWORD = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return str.length() > 0;
		}
	}.setQuestion(String.format(R.STR.getString("question_password"), R.URL.getString("spam_fund_recover"))).setCaseSensitive(true);

    public static final UIQuestion Q_EMAIL = new UIQuestion() {
        @Override
        public boolean isAnswer(String str) {
            return VALID_EMAIL_ADDRESS_REGEX .matcher(str).find();
        }
    }.setQuestion(String.format(R.STR.getString("question_email"), R.URL.getString("spam_fund_signup")));

    public static final UIQuestion Q_SPAM_ADDRESS = new UIQuestion() {
        @Override
        public boolean isAnswer(String str) {
            return VALID_TRYTES .matcher(str).find();
        }
    }.setQuestion(R.STR.getString("question_spam_address"));

	public static final UIQuestion Q_SPAM_TAG = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return VALID_TRYTES .matcher(str).find();
		}
	}.setQuestion(R.STR.getString("question_spam_tag"));

	public static final UIQuestion Q_SPAM_MESSAGE = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return true;
		}
	}.setQuestion(R.STR.getString("question_spam_message")).setCaseSensitive(true);
	
	public static final UIQuestion Q_NODES = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			if(str.equals("")) return Configs.getBln(P.NODES_THIRD_PARTY_NODE_LIST); //|| Configs.get(P.NODES_LIST).length()>0; <-- TODO
	        return VALID_NODE_ADDRESS_REGEX .matcher(str).find();
		}
    }.setQuestion(String.format(R.STR.getString("question_add_node"), R.URL.getString("node_format"), R.URL.getString("node_example_1")));
	
	public static final UIQuestion Q_TIME_FORMAT = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return true;
		}
	}.setQuestion(String.format(R.STR.getString("question_time_format"), "HH:mm:ss", "yyyy-MM-dd HH:mm:ss")).setCaseSensitive(true);

    public static final UIQuestion Q_START_MENU = new UIQuestion() {
        @Override
        public boolean isAnswer(String str) {
            return str.equals("s") || str.equals("c") || str.equals("r");
        }
    }.setQuestion(R.STR.getString("question_command"));




	private boolean caseSensitive = false;
	private String question;
	
	public abstract boolean isAnswer(String str);
	
	public UIQuestion setQuestion(String question) {
		this.question = question;
		return this;
	}

	public String getQuestion() {
		return question;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public UIQuestion setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		return this;
	}
}