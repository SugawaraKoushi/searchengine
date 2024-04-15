package searchengine.businessLogic;

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
    private final PageDao pageDao = new PageDao();
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
        try {
            logger.info("Start parsing: {}", site.getUrl());
            long start = System.currentTimeMillis();
            Site s = siteDao.get(createSiteInstance(site)).orElse(null);
            if (s != null) {
                deleteSiteIndexedData(s);
                s.setStatus(Status.INDEXING);
                s.setStatusTime(new Date(System.currentTimeMillis()));
            } else {
                s = createSiteInstance(site);
            }

            siteDao.saveOrUpdate(s);
            HashSet<Page> pages = getPagesFromSite(s);

            if (pages != null) {
                logger.info("Start saving pages:\t{}", site.getUrl());
                indexPages(pages, s);
                logger.info("End saving pages:\t{}", site.getUrl());
                logger.info("Start saving lemmas:\t{}", site.getUrl());
                saveAndClearCurrentSiteLemmas(PageIndexer.getLemmas(), s);
                logger.info("End saving lemmas:\t{}", site.getUrl());
                logger.info("Start saving indexes: {}", site.getUrl());
                saveAndClearCurrentSiteIndexes(PageIndexer.getIndexes(), s);
                logger.info("End saving indexes:\t{}", site.getUrl());
            }

            if (stop) return;

            s.setStatus(Status.INDEXED);
            siteDao.saveOrUpdate(s);
            logger.info("End parsing: {}. It took {} ms", site.getUrl(), (System.currentTimeMillis() - start));
            IndexingServiceImpl.setStarted(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Останавливает полную индексацю сайта.
     */
    public void stopParsing() {
        stop = true;
        parser.setStop(true);
    }

    private synchronized void deleteSiteIndexedData(Site site) {
        List<Page> pages = pageDao.getAllBySite(site).orElse(new ArrayList<>());
        List<Lemma> lemmas = lemmaDao.getAll().orElse(new ArrayList<>())
                .stream()
                .filter(l -> l.getSite().equals(site))
                .toList();
        List<Index> indexes = indexDao.getAll().orElse(new ArrayList<>())
                .stream()
                .filter(i -> pages.stream().anyMatch(p -> p.getId() == i.getPage().getId()))
                .toList();

        indexDao.delete(indexes.stream().map(Index::getId).toArray());
        lemmaDao.delete(lemmas.stream().map(Lemma::getId).toArray());
        pageDao.delete(pages.stream().map(Page::getId).toArray());
    }

    private Site createSiteInstance(searchengine.config.Site site) {
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

    private void indexPages(HashSet<Page> pages, Site site) {
        pages.removeIf(page -> page.getContent() == null);
        site.setPages(pages);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<PageIndexer> pageIndexers = new ArrayList<>();

        for (Page page : pages) {
            pageIndexers.add(new PageIndexer(site, page));
        }

        try {
            List<Future<Integer>> futures = executor.invokeAll(pageIndexers);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        executorShutdown(executor);
    }

    private void saveAndClearCurrentSiteLemmas(Map<String, Lemma> lemmasMap, searchengine.model.Site site) {
        List<Lemma> lemmas = lemmasMap.values().stream()
                .filter(l -> l.getSite().getId() == site.getId()).toList();
        lemmaDao.saveOrUpdateBatch(lemmas);
        lemmasMap.entrySet().removeIf(e -> e.getValue().getSite().getId() == site.getId());
    }

    private void saveAndClearCurrentSiteIndexes(Collection<Index> indexes, searchengine.model.Site site) {
        List<Index> indexList = indexes.stream()
                .filter(index -> index.getPage().getSite().getId() == site.getId()).toList();
        indexDao.saveOrUpdateBatch(indexList);
        indexes.removeIf(i -> i.getPage().getSite().getId() == site.getId());
    }
}
