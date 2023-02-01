package searchengine;

import searchengine.dto.indexing.PageParser;
import searchengine.model.Page;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

public class Test {
    public static void main(String[] args) {
        PageParser pageParser = new PageParser("https://www.playback.ru");

        HashSet<Page> pages = new ForkJoinPool().invoke(pageParser);

        System.out.println(pages.size());
    }
}
