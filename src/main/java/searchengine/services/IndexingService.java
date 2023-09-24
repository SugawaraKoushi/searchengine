package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.config.Site;
import searchengine.dto.indexing.search.SearchResponse;

public interface IndexingService {
    int startIndexing();

    int stopIndexing();

    int indexPage(String url);

    SearchResponse search(String query, String site, @Nullable int offset, @Nullable int limit);
}
