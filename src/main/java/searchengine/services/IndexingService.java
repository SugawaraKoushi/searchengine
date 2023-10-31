package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.dto.indexing.Response.Response;
import searchengine.dto.indexing.Response.SearchSuccessResponse;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    Response indexPage(String url);

    Response search(String query, String site, @Nullable int offset, @Nullable int limit);
}
