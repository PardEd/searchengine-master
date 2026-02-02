package searchengine.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<SiteEntity> sitesList = siteRepository.getAllSite();
        total.setSites(sitesList.size());
        total.setPages(pageRepository.getSizePages());
        total.setLemmas(lemmaRepository.getSizeLemmas());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (SiteEntity siteModel : sitesList) {
            if (siteModel.getStatus().equals(Status.INDEXING)) {
                total.setIndexing(true);
            }
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(siteModel.getUrl());
            item.setName(siteModel.getName());
            item.setPages(pageRepository.getSizePagesBySiteId(siteModel.getId()));
            item.setLemmas(lemmaRepository.getSizeLemmasBySiteId(siteModel.getId()));
            item.setStatus(siteModel.getStatus().toString());
            if (siteModel.getLastError() != null) {
                item.setError(siteModel.getLastError());
            } else {
                item.setError("");
            }
            item.setStatusTime(Timestamp.valueOf(siteModel.getStatusTime()).getTime());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
