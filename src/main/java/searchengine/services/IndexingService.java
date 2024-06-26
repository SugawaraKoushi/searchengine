package searchengine.services;

import searchengine.dto.response.Response;

public interface IndexingService {
    Response startIndexing();

    Response stopIndexing();

    Response index(String url);
}
