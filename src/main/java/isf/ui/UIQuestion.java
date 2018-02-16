package isf.ui;

import java.util.regex.Pattern;

import isf.Configs;
import isf.P;

public abstract class UIQuestion {

	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.-]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	
	public static final UIQuestion Q_PASSWORD = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return str.length() > 0;
		}
	}.setQuestion("what is your password? (recover it on http://iotaspam.com/account?p=recover)").setCaseSensitive(true);
	
	public static final UIQuestion Q_EMAIL = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
	        return VALID_EMAIL_ADDRESS_REGEX .matcher(str).find();
		}
	}.setQuestion("what is your email address? (sign up on http://iotaspam.com/account?p=signup)");
	
	public static final UIQuestion Q_NODES = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			if(str.equals("")) return Configs.getBln(P.NODES_THIRD_PARTY_LIST) || Configs.get(P.NODES_LIST).length()>0;
	        return VALID_NODE_ADDRESS_REGEX .matcher(str).find();
		}
	}.setQuestion("please enter the API port of the node you want to add [format: 'protocol://host:port', e.g. 'http://node.example.org:14265']");
	
	public static final UIQuestion Q_TIME_FORMAT = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return true;
		}
	}.setQuestion("what time format do you wish for the logger [e.g. 'HH:mm:ss' or 'yyyy-MM-dd HH:mm:ss']").setCaseSensitive(true);
	
	public static final UIQuestion Q_START_MENU = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return str.equals("start") || str.equals("debug") || str.equals("rewards") || str.equals("config");
		}
	}.setQuestion("Please enter a command [start/rewards/config/debug]");
	
	private boolean hideInput = false, caseSensitive = false;
	private String question;
	
	public abstract boolean isAnswer(String str);
	
	public boolean hidesInput() {
		return hideInput;
	}
	
	public UIQuestion hideInput(boolean hideInput) {
		this.hideInput = hideInput;
		return this;
	}
	
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