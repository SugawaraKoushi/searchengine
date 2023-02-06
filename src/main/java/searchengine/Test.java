package searchengine;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SiteParser;
import searchengine.model.Page;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

public class Test {
    public static void main(String[] args) {
        SitesList sitesList = new SitesList();

        IndexingService indexingService = new IndexingServiceImpl(sitesList);
        int response = indexingService.index();
        System.out.println(response);
    }
}
