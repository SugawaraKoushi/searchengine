package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.dao.*;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalInt;

@RequiredArgsConstructor
public class PageIndexer {
    private final Dao<Lemma> lemmaDao = new LemmaDao();
    private final Dao<Index> indexDao = new IndexDao();
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();
    private final String root;
    private final String path;
    private static final LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    public void index() {
        int siteId = getSiteId();

        if (siteId != -1) {
            int pageId = getPageId(siteId);

            if (pageId == -1) {
                //...
            }
        }

        String text = getTextFromPage();
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);

    }

    private String getTextFromPage() {
        String text = "";

        try {
            Document doc = Jsoup.connect(root + path)
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

    private int getSiteId() {
        Site s = new Site();
        s.setUrl(root);
        Optional<Site> optional = siteDao.get(s);

        return optional.map(Site::getId).orElse(-1);
    }

    private int getPageId(int siteId) {
        Site s = new Site();
        s.setId(siteId);

        Page p = new Page();
        p.setSite(s);
        p.setPath(path);

        Optional<Page> optional = pageDao.get(p);
        return optional.map(Page::getId).orElse(-1);
    }
}