package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.PageIndexer;
import searchengine.dto.indexing.SiteParserHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<root>https?://[^/]+)?(?<path>.+)");
    private boolean isStarted = false;
    private final SitesList sites;
    private List<SiteParserHandler> handlers;
    private List<Thread> threads;

    public static int id;

    @Override
    public int startIndexing() {
        handlers = new ArrayList<>();
        threads = new ArrayList<>();

        if (isStarted)
            return -1;

        isStarted = true;
        createSiteParserHandlers();

        for (SiteParserHandler handler : handlers) {
            threads.add(new Thread(handler));
        }

        for (Thread thread : threads) {
            thread.start();
        }

//        isStarted = false;

        return 0;
    }

    @Override
    public int stopIndexing() {
        if (!isStarted)
            return -1;

        for (SiteParserHandler handler : handlers) {
            handler.stopParsing();
        }

        for (Thread thread : threads) {
            thread.interrupt();
        }

        isStarted = false;
        return 0;
    }

    @Override
    public int indexPage(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        Site s = new Site();

        if (matcher.find()) {
            List<Site> sortedSites = sites.getSites().stream().sorted().toList();
            s.setUrl(matcher.group("root"));
            int index = Collections.binarySearch(sortedSites, s);

            if (index > -1) {
                String root = matcher.group("root");
                String path = matcher.group("path");
                PageIndexer indexer = new PageIndexer(root, path);
                indexer.index();

                return 0;
            }
        }

        return -1;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }
}