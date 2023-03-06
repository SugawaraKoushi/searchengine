package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;

@RequiredArgsConstructor
public class PageIndexer {
    private final String url;
    private static final LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    public void index() {
        String text = getTextFromPage();
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);
    }

    private String getTextFromPage() {
        String text = "";

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("BobTheSearcherBot")
                    .referrer("http://www.google.com")
                    .timeout(60000)
                    .get();
            text = doc.text();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return text;
    }
}