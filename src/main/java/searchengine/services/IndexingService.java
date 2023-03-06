package searchengine.services;

public interface IndexingService {
    int startIndexing();

    int stopIndexing();

    int indexPage(String url);
}
