package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.dto.response.Response;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    Response indexPage(String url);
}
