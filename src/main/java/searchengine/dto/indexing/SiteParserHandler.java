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
    private searchengine.config.Site site;
    private SiteParser parser;
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();

    public SiteParserHandler(searchengine.config.Site site) {
        this.site = site;
    }

    private HashSet<Page> getPagesFromSite(Site site) {
        this.parser = new SiteParser(site);
        return new ForkJoinPool().invoke(parser);
    }

    private void saveSite(searchengine.config.Site site) {
        Site s = new Site();
        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());

        if (isSiteExists(s.getId())) {
            siteDao.update(s);
        } else {
            siteDao.save(s);
        }

        HashSet<Page> pages = getPagesFromSite(s);
        pages.removeIf(page -> page.getContent() == null);

        int savePagesStatus = savePages(pages);

        if (savePagesStatus == 0) {
            s.setStatus(Status.INDEXED);
            s.setPages(pages);
        } else {
            s.setStatus(Status.FAILED);
        }

        siteDao.update(s);
    }

    private boolean isSiteExists(int id) {
        return siteDao.get(id).isPresent();
    }

    private boolean isPageExists(int id) {
        return pageDao.get(id).isPresent();
    }

    private int savePages(HashSet<Page> pages) {
        if (pages != null) {
            for (Page p : pages) {
                if (!isPageExists(p.getId()))
                    pageDao.save(p);
            }

            return 0;
        }

        return -1;
    }

    @Override
    public void run() {
        saveSite(site);
    }

    public void stopParsing() {
        parser.stop();
    }
}
