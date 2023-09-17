package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.dao.*;
import searchengine.model.*;

import javax.transaction.Transactional;
import java.util.*;

@RequiredArgsConstructor
public class PageIndexer {
    private final LemmaDao lemmaDao = new LemmaDao();
    private final IndexDao indexDao = new IndexDao();
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();

    private Site site;
    private Page page;

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
            if(page.getSite() == null) {
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
            page = savePage();

            if (page.getCode() >= 400) {
                return;
            }
        } else {
            page = savePage();
        }

        String text = getTextFromPage(page.getContent());
        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(text);

        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            List<Lemma> lemmasToSave = new ArrayList<>();
            List<Lemma> lemmasToUpdate = new ArrayList<>();
            List<Lemma> lemmas = new ArrayList<>();
            List<Index> indexes = new ArrayList<>();

            Lemma l = getLemma(entry.getKey());
            if (l == null) {
                //l = saveLemma(entry.getKey(), entry.getValue());
                l = createLemma(entry.getKey(), entry.getValue());
                lemmasToSave.add(l);
            } else {
                l.setFrequency(l.getFrequency() + entry.getValue());
                lemmasToUpdate.add(l);
                //lemmaDao.update(l);
            }

            lemmaDao.saveBatch(lemmasToSave);
            lemmaDao.updateBatch(lemmasToUpdate);
            lemmas.addAll(lemmasToSave);
            lemmas.addAll(lemmasToUpdate);

            for (Lemma lemma : lemmas) {
                indexes.add(createIndex(lemma));
            }

            indexDao.saveBatch(indexes);

            //saveIndex(l);
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

    private Page savePage() {
        Page p = new Page();
        p.setSite(site);
        p.setPath(page.getPath());
        p.setCode(page.getCode());
        p.setContent(page.getContent());

        pageDao.save(p);
        return p;
    }

    private Lemma createLemma(String lemma, int frequency) {
        Lemma l = new Lemma();
        l.setSite(site);
        l.setFrequency(frequency);
        l.setLemma(lemma);
        return l;
    }

    private Lemma saveLemma(String lemma, int frequency) {
        Lemma l = new Lemma();
        l.setSite(site);
        l.setFrequency(frequency);
        l.setLemma(lemma);
        lemmaDao.save(l);
        return l;
    }

    private void saveIndex(Lemma lemma) {
        Index i = new Index();
        i.setPage(page);
        i.setLemma(lemma);
        i.setRank(lemma.getFrequency());
        indexDao.save(i);
    }

    private void updateSite(String error, Status status) {
        site.setLastError(error);
        site.setStatus(status);
        site.setStatusTime(new Date());

        siteDao.update(site);
    }

    private Index createIndex(Lemma lemma) {
        Index index = new Index();
        index.setPage(page);
        index.setLemma(lemma);
        index.setRank(lemma.getFrequency());
        return index;
    }

    @Transactional
    private void deleteLemmas() {
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

            //indexDao.delete(index);

            if (frequency <= 0) {
                lemmaDao.delete(lemma);
            } else {
                lemma.setFrequency(frequency);
                lemmaDao.update(lemma);
            }
        }
    }

    private void deleteIndexes() {
        Optional<List<Index>> optional = indexDao.getList(page);
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
}