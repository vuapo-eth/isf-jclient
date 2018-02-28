package isf.ui;

public class UIQuestionInt extends UIQuestion {

	public static final UIQuestionInt Q_THREADS_AMOUNT_POW = new UIQuestionInt(1, 1, true);
	public static final UIQuestionInt Q_THREADS_GTTARS_SIZE = new UIQuestionInt(1, -1, true).setQuestion("how many getTransactionsToApprove() responses do you want to queue in advance?").setRecommended(5);
	public static final UIQuestionInt Q_THREADS_PRIORITY_POW = new UIQuestionInt(1, 3, false).setQuestion("how much computational resources do you want to give each thread [1 = minimum, 2 = normal, 3 = maximum]");
	public static final UIQuestionInt Q_SAVE_ACCOUNT_DATA = new UIQuestionInt(0, 2, false).setQuestion("do you want to save your account data in your config.ini? [0 = no; 1 = save only email; 2 = save email and password]");
	public static final UIQuestionInt Q_LOG_INTERVAL = new UIQuestionInt(1, 3600, true).setQuestion("how many seconds do you wish the logger to wait between each info log").setRecommended(60);
	public static final UIQuestionInt Q_NODES_AMOUNT_ROTATION = new UIQuestionInt(1, -1, true).setQuestion("between how many nodes do you want to rotate after each transaction? (use many, but no more than you have nodes)");
	
	private int min, max, recommended;
	private final boolean includeRangeInQuestion;
	
	public UIQuestionInt(int min, int max, boolean includeRangeInQuestion) {
		setRange(min, max);
		this.includeRangeInQuestion = includeRangeInQuestion;
	}
	
	@Override
	public UIQuestionInt setQuestion(String question) {
		return (UIQuestionInt)super.setQuestion(question);
	}

	@Override
	public String getQuestion() {
		return super.getQuestion()+(includeRangeInQuestion ? (max >= min ? " [any integer from " +min+" to "+max : " [any integer >= "+min) + (recommended!= 0?", recommended: "+recommended:"")+"]" : "");
	}
	
	public UIQuestionInt setMax(int max) {
		this.max = max;
		return this;
	}
	
	public UIQuestionInt setRecommended(int recommended) {
		this.recommended = recommended;
		return this;
	}
	
	@Override
	public boolean isAnswer(String str) {
		try {
			int n = Integer.parseInt(str);
			return n >= min && (n <= max || max < min);
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public UIQuestionInt setRange(int min, int max) {
		this.min = min;
		this.max = max;
		return this;
	}
}
