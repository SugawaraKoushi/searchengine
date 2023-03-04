package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SiteParserHandler;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;

    public static int id;

    @Override
    public int startIndexing() {
        for (searchengine.config.Site site : sites.getSites()) {
            Thread thread = new Thread(new SiteParserHandler(site));
            thread.start();
        }

        return 0;
    }

    @Override
    public void stopIndexing() {

    }
}
