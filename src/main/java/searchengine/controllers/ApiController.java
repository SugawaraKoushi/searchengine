package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.Response.FailureResponse;
import searchengine.dto.indexing.Response.Response;
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
    public ResponseEntity<Response> startIndexing() {
        int code = 200;
        Response response = indexingService.startIndexing();

        if (response instanceof FailureResponse) {
            code = 500;
        }

        return ResponseEntity.status(code).body(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        int code = 200;
        Response response = indexingService.stopIndexing();

        if (response instanceof FailureResponse) {
            code = 500;
        }

        return ResponseEntity.status(code).body(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(String url) {
        int code = 200;
        Response response = indexingService.indexPage(url);

        if (response instanceof FailureResponse) {
            code = 400;
        }

        return ResponseEntity.status(code).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(String query, int offset, int limit, String site) {
        Response response = indexingService.search(query, site, offset, limit);
        int code = 200;
        if (response instanceof FailureResponse) {
            code = 400;
        }

        return ResponseEntity.status(code).body(response);
    }
}
