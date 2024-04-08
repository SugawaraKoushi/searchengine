package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.FailureResponse;
import searchengine.dto.response.Response;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    /**
     * Получение статистике об индексации
     * @return
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /**
     * Запуск индексации
     * @return
     */
    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        int code = 200;
        Response response = indexingService.startIndexing();

        if (response instanceof FailureResponse) {
            code = 500;
        }

        return ResponseEntity.status(code).body(response);
    }

    /**
     * Остановка индексации
     * @return
     */
    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        int code = 200;
        Response response = indexingService.stopIndexing();

        if (response instanceof FailureResponse) {
            code = 500;
        }

        return ResponseEntity.status(code).body(response);
    }

    /**
     * Индексация отдельной страницы
     * @param url адрес страницы
     * @return
     */
    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(String url) {
        int code = 200;
        Response response = indexingService.indexPage(url);

        if (response instanceof FailureResponse) {
            code = 400;
        }

        return ResponseEntity.status(code).body(response);
    }

    /**
     * Поиск строки по индексированным страницам.
     *
     * @param query  искомая строка;
     * @param site   сайт, по которому осуществляется поиск (если null - по всем сайтам);
     * @param offset сдвиг от начала списка результатов (по умолчанию 0);
     * @param limit  количество найденных страниц, которые нужно отобразить.
     * @return страницы сайтов с найденной искомой строкой.
     */
    @GetMapping("/search")
    public ResponseEntity<Response> search(String query, int offset, int limit, String site) {
        Response response = searchService.search(query, site, offset, limit);
        int code = 200;
        if (response instanceof FailureResponse) {
            code = 404;
        }

        return ResponseEntity.status(code).body(response);
    }
}
