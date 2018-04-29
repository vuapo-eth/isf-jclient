package isf.ui;


public class UIQuestionInt extends UIQuestion {

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    public static final UIQuestionInt Q_NODE_SYNC_CHECK_INTERVAL = new UIQuestionInt(1, 3600, true).setRecommended(240).setQuestion(R.STR.getString("config_question_sync_check_interval"));
    public static final UIQuestionInt Q_SPAM_DEPTH = new UIQuestionInt(1, 5, true).setRecommended(3).setQuestion(R.STR.getString("config_question_depth"));
    public static final UIQuestionInt Q_POW_ABORT_TIME = new UIQuestionInt(1, 120, true).setRecommended(30).setQuestion(R.STR.getString("config_question_pow_abort"));
	public static final UIQuestionInt Q_THREADS_AMOUNT_POW = new UIQuestionInt(1, CORES, true).setRecommended(Math.max(1, CORES-1)).setQuestion(String.format(R.STR.getString("config_question_cores"), CORES));
	public static final UIQuestionInt Q_LOG_INTERVAL = new UIQuestionInt(1, 3600, true).setQuestion(R.STR.getString("config_question_log_interval")).setRecommended(60);
	public static final UIQuestionInt Q_LOG_PERFORMANCE_REPORT_INTERVAL = new UIQuestionInt(1, 3600, true).setQuestion(R.STR.getString("config_question_performance_report")).setRecommended(180);
	public static final UIQuestionInt Q_NODES_AMOUNT_ROTATION = new UIQuestionInt(1, -1, true).setQuestion(R.STR.getString("config_question_node_rotation")).setRecommended(20);
	public static final UIQuestionInt Q_NODES_THIRD_PARTY_NODE_LIST_RELOAD_INTERVAL = new UIQuestionInt(900, -1, true).setQuestion(R.STR.getString("config_question_third_party_node_list_reload_interval")).setRecommended(1800);


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

    public int getRecommended() {
	    return recommended;
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
