package searchengine;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.LemmaFinder;
import searchengine.dto.indexing.PageIndexer;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Test {
    public static void main(String[] args) {
//        List<Site> sitesList = new ArrayList<>();
//        Site site1 = new Site();
//
//        site1.setName("PlayBack.Ru");
//        site1.setUrl("https://www.playback.ru");
//        sitesList.add(site1);
//
//        SitesList sites = new SitesList();
//        sites.setSites(sitesList);
//
//        IndexingService indexingService = new IndexingServiceImpl(sites);
//        int response = indexingService.startIndexing();
//        System.out.println(response);
        LemmaFinder lf = LemmaFinder.getInstance();
        HashMap<String, Integer> map = lf.getLemmas("Повторное появление леопарда в Осетии позволяет предположить,\n" +
                "что леопард постоянно обитает в некоторых районах Северного\n" +
                "Кавказа.");
        map.forEach((k, v) -> System.out.println(k + " - " + v));
    }
}
