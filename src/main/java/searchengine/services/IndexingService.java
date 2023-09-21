package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.config.Site;

public interface IndexingService {
    int startIndexing();

    int stopIndexing();

    int indexPage(String url);

    int search(String query, String site, @Nullable int offset, @Nullable int limit);
}
