package isf.spam;

import isf.APIManager;
import isf.Configs;
import isf.Main;
import isf.P;
import isf.ui.R;
import isf.ui.UIManager;
import jota.utils.TrytesConverter;
import org.apache.commons.lang3.StringEscapeUtils;
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
                            final String randomWikipediaArticle = randomWikipediaArticle();
                            if(randomWikipediaArticle != null) wikipediaArticles.add(randomWikipediaArticle);
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
        final String trytes = TrytesConverter.toTrytes(transactionMessage.replaceAll("\\n", "\n"));
        transactionMessageTrytes = trytes.substring(0, Math.min(trytes.length(), 2186));
    }
	
	public static String getNextData() {
        final String wikipediaArticle = wikipediaArticles.poll();
        String nextData = transactionMessageTrytes +(wikipediaArticle == null ? "": wikipediaArticle);
        nextData = nextData.substring(0, Math.min(nextData.length(), 2186));
		return nextData;
	}

    public static String randomWikipediaArticle() {

        final String jsonStr = APIManager.request(String.format(R.URL.getString("wikipedia_random"),(2186-transactionMessageTrytes.length())/2-120), null);
        final JSONObject pages = new JSONObject(jsonStr).getJSONObject("query").getJSONObject("pages");
        final JSONObject page = pages.getJSONObject(pages.keys().next());

        final String title = page.getString("title").toUpperCase();
        final String excerpt = page.getString("extract");
        final String trytes = TrytesConverter.toTrytes("\n\n'" + title + "'\n\n" + excerpt.replace("–", "-"));

        return trytes;
    }
}