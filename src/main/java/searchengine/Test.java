package searchengine;

import searchengine.dto.indexing.SiteParser;
import searchengine.model.Page;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

public class Test {
    public static void main(String[] args) {
        SiteParser siteParser = new SiteParser("https://www.playback.ru");

        HashSet<Page> pages = new ForkJoinPool().invoke(siteParser);

        System.out.println(pages.size());
    }
}
