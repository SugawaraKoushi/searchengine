package searchengine.businessLogic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import searchengine.dao.*;
import searchengine.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class PageIndexer implements Callable<Integer> {
    @Getter
    private static ConcurrentHashMap<String, Lemma> lemmas = new ConcurrentHashMap<>();
    @Getter
    private static CopyOnWriteArrayList<Index> indexes = new CopyOnWriteArrayList<>();
    private final LemmaDao lemmaDao = LemmaDao.getInstance();
    private final IndexDao indexDao = new IndexDao();
    private final PageDao pageDao = new PageDao();
    private final SiteDao siteDao = new SiteDao();
    private Site site;
    private Page page;
    private static final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    public PageIndexer(Site site, Page page) {
        this.site = site;
        this.page = page;
    }

    /**
     * Полная индексация страницы
     */
    public void index() {
        getOrCreateSite();
        Page p = getPage();

        if (p != null) {
            deleteOrDecreaseLemma(p);
            deleteIndexes(p);
            deletePage(p);
        }

        if (page.getCode() == 0) {
            p = new Page();
            p.setSite(site);
            p.setPath(page.getPath());
            SiteParser.parsePage(p);
            pageDao.saveOrUpdate(p);
            page = p;

            if (page.getCode() >= 400) {
                return;
            }
        } else {
            pageDao.saveOrUpdate(page);
        }

        String text = getTextFromPage(page.getContent());
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);
        collectLemmasAndIndexes(lemmasMap);
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

    private String getTextFromPage(String content) {
        return Jsoup.parse(content).text();
    }

    private Page getPage() {
        return pageDao.get(page).orElse(null);
    }

    private void getOrCreateSite() {
        Site s = siteDao.get(site).orElse(null);
        if (s == null) {
            site.setName(site.getUrl());
            siteDao.save(site);
        } else {
            site = s;
            if (page.getSite() == null) {
                page.setSite(site);
            }
        }
    }

    private Lemma createLemma(String lemma, int frequency) {
        Lemma l = new Lemma();
        l.setSite(site);
        l.setFrequency(frequency);
        l.setLemma(lemma);
        return l;
    }

    private Index createIndex(Lemma lemma, float rank) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(rank);
        return index;
    }

    private void deleteOrDecreaseLemma(Page p) {
        List<Index> indexes = indexDao.getListByPage(p).orElse(new ArrayList<>());

        if (indexes.isEmpty()) {
            return;
        }

        for (Index index : indexes) {
            Lemma lemma = lemmaDao.get(index.getLemma().getId()).orElse(null);

            if (lemma == null) {
                return;
            }

            int frequency = lemma.getFrequency() - Math.round(index.getRank());

            if (frequency <= 0) {
                indexDao.delete(index);
                lemmaDao.delete(lemma);
            } else {
                lemma.setFrequency(frequency);
                lemmaDao.update(lemma);
            }
        }
    }

    private synchronized void deleteIndexes(Page p) {
        Optional<List<Index>> optional = indexDao.getListByPage(p);
        List<Index> indexes = optional.orElse(new ArrayList<>());

        if (indexes.isEmpty()) {
            return;
        }

        for (Index index : indexes) {
            indexDao.delete(index);
        }
    }

    private void deletePage(Page p) {
        pageDao.delete(p);
    }

    private void collectLemmasAndIndexes(HashMap<String, Integer> lemmasMap) {
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

            if (lemma.getSite().getId() == page.getSite().getId()) {
                index = createIndex(lemma, entry.getValue());
                indexes.add(index);
            }
        }
    }
}