package isf.spam;

import isf.APIManager;
import isf.Configs;
import isf.Main;
import isf.P;
import isf.ui.R;
import isf.ui.UIManager;
import jota.utils.TrytesConverter;
import org.json.JSONObject;

import java.util.LinkedList;

public class UploadDataManager {

    private static UIManager UIM = new UIManager("UpldData");
	private static LinkedList<String> wikipediaArticles = new LinkedList<>();

	private static String transactionMessageTrytes = TrytesConverter.toTrytes("Get paid in iotas for supporting the tangle network by spamming transactions. For more information, visit: http://iotaspam.com/.");

	public static void start() {
	    if(Configs.getBln(P.SPAM_WIKIPEDIA_MESSAGE)) new Thread(Main.SUPER_THREAD, "UploadDataManager") {
            @Override
            public void run() {
                while(true) {
                    if(wikipediaArticles.size() < 10) {
                        try {
                            String am = randomWikipediaArticle();
                            if(am != null) wikipediaArticles.add(am);
                        } catch (Throwable t) {
                            UIM.logException(t, false);
                        }
                    }
                    else try { sleep(1000); } catch (InterruptedException e) {}
                }
            }
        }.start();
    }

    public static void setTransactionMessage(String transactionMessage) {
        final String trytes = TrytesConverter.toTrytes(transactionMessage);
        transactionMessageTrytes = trytes.substring(0, Math.min(trytes.length(), 2186));
    }
	
	public static String getNextData() {
        String wikipediaArticle = wikipediaArticles.poll();
        String nextData = transactionMessageTrytes +(wikipediaArticle == null ? "": wikipediaArticle);
        nextData = nextData.substring(0, Math.min(nextData.length(), 2186));
		return nextData;
	}

    public static String randomWikipediaArticle() {

        String jsonStr = APIManager.request(String.format(R.URL.getString("wikipedia_random"),(2186-transactionMessageTrytes.length())/2-120), null);
        JSONObject pages = new JSONObject(jsonStr).getJSONObject("query").getJSONObject("pages");
        JSONObject page = pages.getJSONObject(pages.keys().next());

        String title = page.getString("title").toUpperCase();
        String excerpt = page.getString("extract");
        return TrytesConverter.toTrytes(" >>>>> random wikipedia article: " + title + " >>>>> " + excerpt.replace("–", "-"));
    }
}