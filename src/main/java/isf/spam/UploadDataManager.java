package isf.spam;

import isf.APIManager;
import isf.Configs;
import isf.P;
import isf.ui.R;
import isf.ui.UIManager;
import jota.utils.TrytesConverter;
import org.json.JSONObject;

import java.util.LinkedList;

public class UploadDataManager {

    private static UIManager UIM = new UIManager("UpldData");
	private static LinkedList<String> wikipediaArticles = new LinkedList<>();

	public static void start() {
	    if(Configs.getBln(P.SPAM_WIKIPEDIA_MESSAGE)) new Thread() {
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
	
	public static String getNextData() {
        String wikipediaArticle = wikipediaArticles.poll();
		return TrytesConverter.toTrytes("Get paid in iotas for supporting the tangle network by spamming transactions. For more information, visit: http://iotaspam.com/."
                +(wikipediaArticle == null ? "":" >>>>> random wikipedia article: "+wikipediaArticle));
	}

    public static String randomWikipediaArticle() {

        String jsonStr = APIManager.request(R.URL.getString("wikipedia_random"), null);
        JSONObject pages = new JSONObject(jsonStr).getJSONObject("query").getJSONObject("pages");
        JSONObject page = pages.getJSONObject(pages.keys().next());

        String title = page.getString("title").toUpperCase();
        String excerpt = page.getString("extract");
        return title + " >>>>> " + excerpt.replace("–", "-");
    }
}