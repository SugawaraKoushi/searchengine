package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.dao.*;
import searchengine.model.*;

import java.util.*;

@RequiredArgsConstructor
public class PageIndexer {
    private final Dao<Lemma> lemmaDao = new LemmaDao();
    private final IndexDao indexDao = new IndexDao();
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();
    private final Site site;
    private final String path;
    private static final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    public void index() {
        Site site = getSite();

        if (site == null) {
            saveSite();
        }

        Page page = getPage(path, site);

        if (page != null) {
            deleteLemmas(page);
            deleteIndexes(page);
            deletePage(page);
        }

        page = parsePage(path);
        page.setSite(site);
        savePage(page);

        if (page.getCode() >= 400) {
            return;
        }

        String text = getTextFromPage(page.getContent());
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);

        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            Lemma lemma = new Lemma();
            lemma.setSite(site);
            lemma.setLemma(entry.getKey());
            lemma.setFrequency(entry.getValue());

            Optional<Lemma> optional = lemmaDao.get(lemma);
            Lemma l = optional.orElse(null);

            if (l == null) {
                lemmaDao.save(lemma);
            } else {
                lemma = l;
                lemma.setFrequency(lemma.getFrequency() + entry.getValue());
                lemmaDao.update(lemma);
            }

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(lemma.getFrequency());
            indexDao.save(index);
        }
    }

    private Page parsePage(String path) {
        Page page = new Page();
        int code = 400;
        Connection.Response response = null;

        page.setPath(path);

        try {
            response = Jsoup.connect(site.getUrl() + path)
                    .userAgent("BobTheSearcherBot")
                    .referrer("http://www.google.com")
                    .timeout(60000)
                    .execute();
            Document doc = response.parse();

            code = response.statusCode();
            String content = doc.toString();
            content = content.replaceAll("'", "\\\\'");
            content = content.replaceAll("\"", "\\\\\"");
            page.setContent(content);

        } catch (Exception e) {
            if (response != null) {
                code = response.statusCode();
            }

            updateSite(e.getMessage(), Status.FAILED);
        } finally {
            page.setCode(code);
            updateSite(null, Status.INDEXED);
        }

        return page;
    }

    private String getTextFromPage(String content) {
        return Jsoup.parse(content).text();
    }

    private Site getSite() {
        return siteDao.get(site).orElse(null);
    }

    private Page getPage(String path, Site site) {
        Page page = new Page();
        page.setPath(path);
        page.setSite(site);

        Optional<Page> optional = pageDao.get(page);

        return optional.orElse(null);
    }

    private void saveSite() {
        siteDao.save(site);
    }

    private void updateSite(String error, Status status) {
        Site site = getSite();
        site.setLastError(error);
        site.setStatus(status);
        site.setStatusTime(new Date());

        siteDao.update(site);
    }

    private void savePage(Page page) {
        pageDao.save(page);
    }

    private void deleteLemmas(Page page) {
        Optional<List<Index>> opt1 = indexDao.getList(page);
        List<Index> indexes = opt1.orElse(new ArrayList<>());

        if (indexes.isEmpty()) {
            return;
        }

        for (Index index : indexes) {
            Optional<Lemma> opt2 = lemmaDao.get(index.getLemma().getId());
            Lemma lemma = opt2.orElse(null);

            if (lemma == null) {
                return;
            }

            int frequency = lemma.getFrequency() - Math.round(index.getRank());

            if (frequency < 0) {
                lemmaDao.delete(lemma);
            } else {
                lemma.setFrequency(frequency);
                lemmaDao.update(lemma);
            }
        }
    }

    private void deleteIndexes(Page page) {
        Optional<List<Index>> optional = indexDao.getList(page);
        List<Index> indexes = optional.orElse(new ArrayList<>());

        if (indexes.isEmpty()) {
            return;
        }

        for (Index index: indexes) {
            indexDao.delete(index);
        }
    }

    private void deletePage(Page page)  {
        pageDao.delete(page);
    }
}