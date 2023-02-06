package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    private searchengine.config.Site site;
    private Dao<Page> pageDao = new PageDao();
    private Dao<Site> siteDao = new SiteDao();


    @Override
    public int index() {
        return 0;
    }

    private HashSet<Page> getPagesFromSite() {
        SiteParser parser = new SiteParser(this.site.getUrl());
        return new ForkJoinPool().invoke(parser);
    }

    private void saveSite() {
        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(new Date(System.currentTimeMillis()));
        site.setLastError(null);
        site.setUrl(this.site.getUrl());
        site.setName(this.site.getName());
        site.setPages(getPagesFromSite());


    }
}
