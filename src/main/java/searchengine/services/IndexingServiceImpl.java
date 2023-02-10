package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dao.Dao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.SiteParser;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final Dao<Page> pageDao = new PageDao();
    private final Dao<Site> siteDao = new SiteDao();


    @Override
    public int startIndexing() {
        for (searchengine.config.Site site : sites.getSites()) {
            saveSite(site);
        }
        return 0;
    }

    private HashSet<Page> getPagesFromSite(Site site) {
        SiteParser parser = new SiteParser(site);
        return new ForkJoinPool().invoke(parser);
    }

    private void saveSite(searchengine.config.Site site) {
        Site s = new Site();

        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());

        HashSet<Page> pages = getPagesFromSite(s);
        siteDao.save(s);

        if (pages != null){
            s.setPages(pages);
            pageDao.saveAll(pages);
        }
    }
}
