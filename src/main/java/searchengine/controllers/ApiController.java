package searchengine.controllers;

import java.net.URISyntaxException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
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

    public ApiController(StatisticsService statisticsService,
        IndexingService indexingService,
        SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResponse getStatistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.OK)
    public IndexingResponse indexPage(@RequestParam @NotEmpty String url)
        throws URISyntaxException {

        validateUrl(url);
        return indexingService.indexPage(url);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponse search(
        @RequestParam @NotEmpty String query,
        @RequestParam(required = false) String site,
        @RequestParam @PositiveOrZero int offset,
        @RequestParam @Positive int limit
    ) {
        validateSearchParams(query, offset, limit);
        return searchService.search(query, site, offset, limit);
    }

    private void validateUrl(String url) throws URISyntaxException {
        new java.net.URI(url);
    }

    private void validateSearchParams(String query, int offset, int limit) {
        if (limit > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Превышен максимальный лимит результатов (100)"
            );
        }
        if (offset < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Смещение не может быть отрицательным"
            );
        }
    }
}
