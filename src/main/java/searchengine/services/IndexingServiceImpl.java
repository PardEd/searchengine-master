package searchengine.services;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingAlreadyStartedException;
import searchengine.exceptions.IndexingNotStartedException;
import searchengine.exceptions.IndexingOutsideSitesException;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.components.SiteParser;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;

    private final SiteParser siteParser;

    @Transactional
    @Override
    public IndexingResponse startIndexing() {
        if (siteParser.isIndexing()) {
            throw new IndexingAlreadyStartedException();
        }

        try {
            List<Site> sitesList = sites.getSites();

            for (Site site : sitesList) {
                cleanUpExistingData(site.getName());
                SiteEntity siteEntity = createSite(site);
                siteRepository.saveAndFlush(siteEntity);
                siteParser.parseSite(siteEntity);
            }

            return createPositiveResponse();
        } catch (IndexingAlreadyStartedException e) {
            log.error("Ошибка при индексации {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void cleanUpExistingData(String siteName) {
        List<SiteEntity> existingSites = siteRepository.findSiteByName(siteName);
        if (!existingSites.isEmpty()) {
            siteRepository.deleteAll(existingSites);
        }
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!siteParser.isIndexing()) {
            throw new IndexingNotStartedException();
        }
        siteParser.stopPoolIndexing();
        return createPositiveResponse();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (siteParser.isIndexing()) {
            throw new IndexingAlreadyStartedException();
        }
        Site site = findSiteFromConfig(url);
        if (site == null) {
            throw new IndexingOutsideSitesException();
        }
        cleanUpExistingData(site.getUrl());
        String siteUrl = site.getUrl();
        SiteEntity siteEntity = siteRepository.findSiteByUrl(siteUrl.endsWith("/") ?
            siteUrl.substring(0, siteUrl.length() - 1) : siteUrl);
        if (siteEntity == null) {
            siteEntity = createSite(site);
            siteRepository.saveAndFlush(siteEntity);
        }

        siteParser.parseSite(siteEntity);

        if (!siteEntity.getStatus().equals(Status.FAILED)) {
            siteEntity.setStatus(Status.INDEXED);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.saveAndFlush(siteEntity);
        }
        return createPositiveResponse();
    }

    private SiteEntity createSite(Site siteConfig) {
        SiteEntity siteEntity = new SiteEntity();
        String siteUrl = siteConfig.getUrl();
        siteEntity.setUrl(siteUrl.endsWith("/") ?
            siteUrl.substring(0, siteUrl.length() - 1) : siteUrl);
        siteEntity.setName(siteConfig.getName());
        siteEntity.setStatus(Status.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        return siteEntity;
    }

    private Site findSiteFromConfig(String url) {
        String regex = "^https?://[A-za-zА-яа-я0-9.\\-_/]+$";
        if (!url.matches(regex)) {
            return null;
        }
        return sites.getSites()
            .stream()
            .filter(site -> site.getUrl().contains(url))
            .findFirst()
            .orElse(null);
    }

    private IndexingResponse createPositiveResponse() {
        IndexingResponse indexingResponse = new IndexingResponse();
        indexingResponse.setResult(true);
        return indexingResponse;
    }
}