package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dao.Dao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.SiteParser;
import searchengine.dto.indexing.SiteParserHandler;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();
    private Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    public static int id;

    @Override
    public int startIndexing() {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (searchengine.config.Site site : sites.getSites()) {
            Thread thread = new Thread(new SiteParserHandler(site));
            thread.start();
        }

//        executor.shutdown();
//        try {
//            if(!executor.awaitTermination(800,TimeUnit.MILLISECONDS))
//                executor.shutdown();
//        } catch (InterruptedException e) {
//            executor.shutdownNow();
//        }

        return 0;
    }

    private HashSet<Page> getPagesFromSite(Site site) {
        SiteParser parser = new SiteParser(site);
        return new ForkJoinPool().invoke(parser);
    }

    private void saveSite(searchengine.config.Site site) {
        ++id;
        Site s = new Site();
        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());

        if (!isSiteAlreadyExists(id)) {
            logger.debug("new site");
            siteDao.save(s);
        }


        HashSet<Page> pages = getPagesFromSite(s);

        int pageSaveResult = 0;
        if (pages != null){
            s.setPages(pages);
            pageSaveResult = pageDao.saveAll(pages);
        }

        s.setStatus(pageSaveResult == 0 ? Status.INDEXED : Status.FAILED);
        siteDao.update(s);
    }

    private boolean isSiteAlreadyExists(int id) {
        return siteDao.get(id).isPresent();
    }
}
