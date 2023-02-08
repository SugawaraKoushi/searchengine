package searchengine;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        List<Site> sitesList = new ArrayList<>();
        Site site1 = new Site();
        Site site2 = new Site();
        Site site3 = new Site();

        site1.setName("PlayBack.Ru");
        site1.setUrl("https://www.playback.ru");
        sitesList.add(site1);

        SitesList sites = new SitesList();
        sites.setSites(sitesList);

        IndexingService indexingService = new IndexingServiceImpl(sites);
        int response = indexingService.index();
        System.out.println(response);
    }
}
