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
            Site finalS = s;
            Set<Lemma> currentSiteLemmas = new HashSet<>(
                    PageIndexer.getLemmas().values()
                            .stream()
                            .filter(l -> l.getSite().getId() == finalS.getId())
                            .toList()
            );
            lemmaDao.saveOrUpdateBatch(currentSiteLemmas);
            for (Map.Entry<String, Lemma> entry : PageIndexer.getLemmas().entrySet()) {
                if (currentSiteLemmas.contains(entry.getValue())) {
                    PageIndexer.getLemmas().remove(entry.getKey());
                }
            }

            List<Index> currentSiteIndexes = PageIndexer.getIndexes()
                    .stream()
                    .filter(i -> i.getPage().getSite().getId() == finalS.getId())
                    .toList();
            indexDao.saveOrUpdateBatch(currentSiteIndexes);
            PageIndexer.getIndexes().removeAll(currentSiteIndexes);
        }

        if (stop)
            return;

        s.setStatus(Status.INDEXED);
        saveOrUpdateSite(s);
        logger.info("End parsing: " + site.getUrl());
        logger.info("Parsing " + site.getUrl() + " took " + (System.currentTimeMillis() - start) + " ms");
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

    private void saveOrUpdateSite(Site s) {
        if (isSiteExists(s)) {
            siteDao.update(s);
        } else {
            siteDao.save(s);
        }
    }

    private boolean isSiteExists(Site s) {
        Optional<Site> opt = siteDao.get(s);

        if (opt.isEmpty()) {
            return false;
        }

        s.setId(opt.get().getId());
        s.setLastError(opt.get().getLastError());
        s.setPages(opt.get().getPages());
        return true;
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
}
