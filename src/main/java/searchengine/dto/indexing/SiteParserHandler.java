package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.dao.*;
import searchengine.model.*;
import searchengine.services.IndexingServiceImpl;

import java.util.*;
import java.util.concurrent.*;

@RequiredArgsConstructor
public class SiteParserHandler implements Runnable {
    private searchengine.config.Site site;
    private SiteParser parser;
    private final SiteDao siteDao = new SiteDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final IndexDao indexDao = new IndexDao();
    private boolean stop = false;
    private final Logger logger = LoggerFactory.getLogger(SiteParser.class);

    public SiteParserHandler(searchengine.config.Site site) {
        this.site = site;
    }

    /**
     * Запускает полную индексацию сайта.
     */
    @Override
    public void run() {
        logger.info("Start parsing: " + site.getUrl());
        long start = System.currentTimeMillis();

        Site s = siteDao.get(createSiteInstance(site)).orElse(null);
        if (s != null) {
            s.setStatus(Status.INDEXING);
            s.setStatusTime(new Date());
        } else {
            s = createSiteInstance(site);
        }

        siteDao.saveOrUpdate(s);

        HashSet<Page> pages = getPagesFromSite(s);

        if (pages != null) {
            pages.removeIf(page -> page.getContent() == null);
            s.setPages(pages);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<PageIndexer> pageIndexers = new ArrayList<>();

            for (Page page : pages) {
                pageIndexers.add(new PageIndexer(s, page));
            }

            try {
                List<Future<Integer>> futures = executor.invokeAll(pageIndexers);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            executorShutdown(executor);
            saveAndClearCurrentSiteLemmas(PageIndexer.getLemmas(), s);
            saveAndClearCurrentSiteIndexes(PageIndexer.getIndexes(), s);
        }

        if (stop)
            return;

        s.setStatus(Status.INDEXED);
        siteDao.saveOrUpdate(s);
        logger.info("End parsing: " + site.getUrl());
        logger.info("Parsing " + site.getUrl() + " tooks " + (System.currentTimeMillis() - start) + " ms");
        IndexingServiceImpl.setIsStarted(false);
    }

    /**
     * Останавливает полную индексацю сайта.
     */
    public void stopParsing() {
        stop = true;
        parser.stop();
    }

    /**
     * Создает экземпляр объекта класса model.Site.
     *
     * @param site Данные о сайте из application.yaml
     * @return объект класса model.Site
     */
    public Site createSiteInstance(searchengine.config.Site site) {
        Site s = new Site();
        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());
        return s;
    }

    private HashSet<Page> getPagesFromSite(Site site) {
        this.parser = new SiteParser(site);
        this.parser.setStop(false);
        return new ForkJoinPool().invoke(parser);
    }

    private void executorShutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void saveAndClearCurrentSiteLemmas(Map<String, Lemma> lemmasMap, searchengine.model.Site site) {
        Set<Lemma> lemmasSet = new HashSet<>();
        for (Lemma lemma : lemmasMap.values()) {
            if (lemma.getSite().getId() == site.getId()) {
                lemmasSet.add(lemma);
            }
        }

        lemmaDao.saveOrUpdateBatch(lemmasSet);

        for (Map.Entry<String, Lemma> entry : PageIndexer.getLemmas().entrySet()) {
            if (lemmasSet.contains(entry.getValue())) {
                PageIndexer.getLemmas().remove(entry.getKey());
            }
        }
    }

    private void saveAndClearCurrentSiteIndexes(Collection<Index> indexes, searchengine.model.Site site) {
        Set<Index> indexesSet = new HashSet<>();
        for (Index index : indexes) {
            if (index.getPage().getSite().getId() == site.getId()) {
                indexesSet.add(index);
            }
        }

        indexDao.saveOrUpdateBatch(indexesSet);
        indexes.removeAll(indexesSet);
    }
}
