package searchengine.controllers;

import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.dto.indexing.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        String responseText = indexingService.startIndexing() == 0 ?
                "{ \"result\": true }" : "{ \"result\": false, \"error\": \"Индексация уже запущена\" }";
        return ResponseEntity.ok(responseText);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        String responseText = indexingService.stopIndexing() == 0 ?
                "{ \"result\": true }" : "{ \"result\": false, \"error\": \"Индексация не запущена\" }";
        return ResponseEntity.ok(responseText);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(String url) {
        String responseText;
        int indexPageResponse = indexingService.indexPage(url);

        if (indexPageResponse == 0) {
            responseText = "{ \"result\": true }";
            return ResponseEntity.ok(responseText);
        } else {
            responseText = "{ \"result\": false, " +
                    "\"error\": \"Данная страница находится за пределами сайтов," +
                    "указанных в конфигурационном файле\" }";
            return ResponseEntity.status(400).body(responseText);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(String query, int offset, int limit, String site) {
        SearchResponse response = indexingService.search(query, site, offset, limit);

        return ResponseEntity.ok(response);
    }
}
