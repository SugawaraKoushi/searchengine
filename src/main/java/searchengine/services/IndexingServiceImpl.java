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

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private Dao<Page> pageDao = new PageDao();
    private Dao<Site> siteDao = new SiteDao();


    @Override
    public int index() {
        saveSite(sites.getSites().get(2));
        return 0;
    }

    private HashSet<Page> getPagesFromSite(searchengine.config.Site site) {
        SiteParser parser = new SiteParser(site.getUrl());
        return new ForkJoinPool().invoke(parser);
    }

    private void saveSite(searchengine.config.Site site) {
        Site s = new Site();
        s.setStatus(Status.INDEXING);
        s.setStatusTime(new Date(System.currentTimeMillis()));
        s.setLastError(null);
        s.setUrl(site.getUrl());
        s.setName(site.getName());
        s.setPages(getPagesFromSite(site));

        siteDao.save(s);
    }
}
