package searchengine.dto.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.dao.*;
import searchengine.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@RequiredArgsConstructor
public class PageIndexer implements Callable<Integer> {
    private final LemmaDao lemmaDao = LemmaDao.getInstance();
    private final IndexDao indexDao = new IndexDao();
    private final PageDao pageDao = new PageDao();
    private final SiteDao siteDao = new SiteDao();

    @Getter
    private static ConcurrentHashMap<String, Lemma> lemmas = new ConcurrentHashMap<>();
    @Getter
    private static CopyOnWriteArrayList<Index> indexes = new CopyOnWriteArrayList<>();

    private Site site;
    private Page page;
    private final Logger logger = LoggerFactory.getLogger(SiteParser.class);

    private static final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    public PageIndexer(Site site, Page page) {
        this.site = site;
        this.page = page;
    }

    public void index() {
        Site s = getSite();
        if (s == null) {
            saveSite();
        } else {
            site = s;
            if (page.getSite() == null) {
                page.setSite(site);
            }
        }

        if (page.getCode() == 0) {
            Page p = getPage();
            if (p != null) {
                page = p;
                deleteLemmas();
                deleteIndexes();
                deletePage();
            }

            parsePage();
            pageDao.saveOrUpdate(page);

            if (page.getCode() >= 400) {
                return;
            }
        } else {
            pageDao.saveOrUpdate(page);
        }

        String text = getTextFromPage(page.getContent());
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);

        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            Index index;
            Lemma lemma;

            if (lemmas.containsKey(entry.getKey())) {
                lemma = lemmas.get(entry.getKey());
                int freq = lemma.getFrequency() + entry.getValue();
                lemma.setFrequency(freq);
            } else {
                lemma = createLemma(entry.getKey(), entry.getValue());
            }

            lemmas.compute(entry.getKey(), (k, v) -> lemma);
            index = createIndex(lemma, entry.getValue());
            indexes.add(index);
        }
    }

    private void parsePage() {
        int code = 400;
        Connection.Response response = null;

        try {
            response = Jsoup.connect(site.getUrl() + page.getPath())
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
    }

    private String getTextFromPage(String content) {
        return Jsoup.parse(content).text();
    }

    private Site getSite() {
        return siteDao.get(site).orElse(null);
    }

    private Page getPage() {
        return pageDao.get(page).orElse(null);
    }

    private Lemma getLemma(String lemma) {
        Lemma l = new Lemma();
        l.setLemma(lemma);

        return lemmaDao.get(l).orElse(null);
    }

    private void saveSite() {
        if (site.getName() == null) {
            site.setName(site.getUrl());
        }

        siteDao.save(site);
    }

    private Lemma createLemma(String lemma, int frequency) {
        Lemma l = new Lemma();
        l.setSite(site);
        l.setFrequency(frequency);
        l.setLemma(lemma);
        return l;
    }

    private void updateSite(String error, Status status) {
        site.setLastError(error);
        site.setStatus(status);
        site.setStatusTime(new Date());

        siteDao.update(site);
    }

    private Index createIndex(Lemma lemma, float rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }

    private void deleteLemmas() {
        Optional<List<Index>> opt1 = indexDao.getListByPage(page);
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

            if (frequency <= 0) {
                lemmaDao.delete(lemma);
            } else {
                lemma.setFrequency(frequency);
                lemmaDao.update(lemma);
            }
        }
    }

    private void deleteIndexes() {
        Optional<List<Index>> optional = indexDao.getListByPage(page);
        List<Index> indexes = optional.orElse(new ArrayList<>());

        if (indexes.isEmpty()) {
            return;
        }

        for (Index index : indexes) {
            indexDao.delete(index);
        }
    }

    private void deletePage() {
        pageDao.delete(page);
    }

    @Override
    public Integer call() {
        try {
            index();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}