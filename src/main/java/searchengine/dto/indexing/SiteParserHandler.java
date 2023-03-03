package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.dao.Dao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.IndexingServiceImpl;

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteParserHandler implements Runnable {
    private SiteParser siteParser;
    private searchengine.config.Site site;
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();
    private static volatile int id;
    private Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    public SiteParserHandler(searchengine.config.Site site) {
        this.site = site;
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
        pages.removeIf(page -> page.getContent() == null);

        int pageSaveResult = 0;
        if (pages != null) {
            pageSaveResult = pageDao.saveAll(pages);
            s.setPages(pages);
        }

        s.setStatus(pageSaveResult == 0 ? Status.INDEXED : Status.FAILED);
        siteDao.update(s);
    }

    private boolean isSiteAlreadyExists(int id) {
        return siteDao.get(id).isPresent();
    }

    @Override
    public void run() {
        saveSite(site);
    }
}
