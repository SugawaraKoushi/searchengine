package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import searchengine.dao.Dao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteParserHandler implements Runnable {
    private searchengine.config.Site site;
    private SiteParser parser;
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();
    private boolean stop = false;

    public SiteParserHandler(searchengine.config.Site site) {
        this.site = site;
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

        if (!opt.isPresent()) {
            return false;
        }

        s.setId(opt.get().getId());
        s.setLastError(opt.get().getLastError());
        s.setPages(opt.get().getPages());
        return true;
    }

    private boolean isPageExists(Page p) {
        return pageDao.get(p).isPresent();
    }

    private int savePages(HashSet<Page> pages) {
        if (pages != null) {
            for (Page p : pages) {
                if (!isPageExists(p))
                    pageDao.save(p);
            }

            return 0;
        }

        return -1;
    }

    @Override
    public void run() {
        Site s = createSiteInstance(site);
        saveOrUpdateSite(s);

        HashSet<Page> pages = getPagesFromSite(s);
        if (pages != null) {
            pages.removeIf(page -> page.getContent() == null);
            s.setPages(pages);

            for (Page page : pages) {
                PageIndexer indexer = new PageIndexer(s, page);
                indexer.index();
            }

            //savePages(pages);
            saveOrUpdateSite(s);
        }

        if (stop)
            return;

        s.setStatus(Status.INDEXED);
        saveOrUpdateSite(s);
    }

    public void stopParsing() {
        stop = true;
        parser.stop();
    }

    public Site createSiteInstance(searchengine.config.Site site) {
        Site s = new Site();
        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());

        return s;
    }
}
