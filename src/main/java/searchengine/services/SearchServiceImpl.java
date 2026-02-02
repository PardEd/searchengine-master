package searchengine.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.SearchEmptyTermException;
import searchengine.exceptions.SearchNoReadyIndexException;
import searchengine.exceptions.SearchQueryTooExtensiveException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.components.Morphology;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    @Autowired
    LuceneMorphology luceneMorphology;
    private static final double EXCLUDE_TOP_LEMMAS_PERCENT = 0.001;

    @Transactional
    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        query = query.replaceAll("[^А-яа-я\\s]", "");
        if (query.isBlank()) {
            throw new SearchEmptyTermException();
        }
        List<SiteEntity> siteEntities = findSiteEntity(site);
        List<String> queryLemmas = new Morphology(luceneMorphology).collectLemmas(query)
            .keySet()
            .stream()
            .toList();
        queryLemmas = excludeTopLemmas(queryLemmas);
        if (queryLemmas.isEmpty()) {
            throw new SearchQueryTooExtensiveException();
        }
        Map<Long, Float> pageIdsRanks = new HashMap<>();
        for (SiteEntity siteEntity : siteEntities) {
            List<LemmaEntity> lemmaEntities = convertToLemmaEntity(queryLemmas,
                siteEntity.getId());
            lemmaEntities.sort(Comparator.comparing(LemmaEntity::getFrequency));
            pageIdsRanks.putAll(collectPageIdRank(lemmaEntities));
        }
        if (pageIdsRanks.isEmpty()) {
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setResult(true);
            return searchResponse;
        }
        Map<PageEntity, Float> pageModelsRelevance = calculateRelevance(pageIdsRanks, offset,
            limit);

        return createPositiveResponse(pageModelsRelevance, queryLemmas, pageIdsRanks.size());
    }

    private List<SiteEntity> findSiteEntity(String site) {
        List<SiteEntity> searchSiteModels = new ArrayList<>();
        if (site == null) {
            List<SiteEntity> siteModels = siteRepository.getAllSite();
            for (SiteEntity siteModel : siteModels) {
                if (isSiteReadyForSearch(siteModel)) {
                    searchSiteModels.add(siteModel);
                }
            }
        } else {
            SiteEntity siteModel = siteRepository.findSiteByUrl(site);
            if (isSiteReadyForSearch(siteModel)) {
                searchSiteModels.add(siteModel);
            }
        }
        if (searchSiteModels.isEmpty()) {
            throw new SearchNoReadyIndexException();
        }
        return searchSiteModels;
    }

    private boolean isSiteReadyForSearch(SiteEntity siteModel) {
        switch (siteModel.getStatus()){
            case FAILED -> {
                return false;
            }
            case INDEXING ->
                throw new SearchNoReadyIndexException();

        }
        return true;
    }

    private List<String> excludeTopLemmas(List<String> lemmas) {
        int sizeLemmas = lemmaRepository.getSizeLemmas();
        int cutLemmas = (int) (sizeLemmas * EXCLUDE_TOP_LEMMAS_PERCENT);
        List<String> topLemmas = new ArrayList<>(lemmaRepository.getTopLemma(cutLemmas));
        return lemmas.stream()
            .filter(l -> !topLemmas.contains(l))
            .collect(Collectors.toList());
    }

    private List<LemmaEntity> convertToLemmaEntity(List<String> lemmas, Long siteId) {
        List<LemmaEntity> lemmaModels = new ArrayList<>();
        for (String lemma : lemmas) {
            LemmaEntity lemmaModel = lemmaRepository.findLemmaByNameAndSiteId(lemma, siteId);
            if (lemmaModel != null) {
                lemmaModels.add(lemmaModel);
            }
        }
        return lemmaModels;
    }

    private Map<Long, Float> collectPageIdRank(List<LemmaEntity> queryLemmaModels) {
        Map<Long, Float> pageIdsRank = new HashMap<>();
        for (int i = 0; i < queryLemmaModels.size(); i++) {
            Map<Long, Float> comparedPageIdsRank = new HashMap<>();
            List<IndexEntity> indexModels = indexRepository.findByLemmaId(
                queryLemmaModels.get(i).getId());
            for (IndexEntity indexModel : indexModels) {
                comparedPageIdsRank.put(indexModel.getPageId().getId(), indexModel.getRating());
            }
            if (i == 0) {
                pageIdsRank.putAll(comparedPageIdsRank);
                continue;
            }
            pageIdsRank = pageIdsRank.entrySet().stream()
                .filter(entry -> comparedPageIdsRank.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue() + comparedPageIdsRank.get(entry.getKey())
                ));
        }
        return pageIdsRank;
    }

    private Map<PageEntity, Float> calculateRelevance(Map<Long, Float> pageIdsRanks, int offset,
        int limit) {
        Float maxAbsoluteRelevance = pageIdsRanks
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElseThrow(NullPointerException::new)
            .getValue();

        pageIdsRanks = pageIdsRanks.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue() / maxAbsoluteRelevance
        ));

        Map<PageEntity, Float> pageModelsRelevance = new LinkedHashMap<>();
        pageIdsRanks.entrySet().stream()
            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
            .skip(offset)
            .limit(limit)
            .forEachOrdered(entry -> pageModelsRelevance.put(
                pageRepository.findById(entry.getKey()).orElseThrow(NullPointerException::new),
                entry.getValue()));

        return pageModelsRelevance;
    }

    private SearchResponse createPositiveResponse(Map<PageEntity, Float> relevancePages,
        List<String> queryLemmas, int countRelevancePages) {
        List<DataSearchItem> data = new ArrayList<>();
        for (Map.Entry<PageEntity, Float> entry : relevancePages.entrySet()) {
            PageEntity pageEntity = entry.getKey();
            DataSearchItem item = new DataSearchItem();
            item.setSite("http://" + pageEntity.getSiteId().getName() + pageEntity.getPath());
            item.setUri("");
            item.setSiteName(pageEntity.getSiteId().getName());
            item.setTitle(getPageTitle(pageEntity));
            item.setSnippet(createSnippet(pageEntity, queryLemmas));
            item.setRelevance(entry.getValue());
            data.add(item);
        }
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        searchResponse.setCount(countRelevancePages);
        searchResponse.setData(data);
        return searchResponse;
    }

    private String getPageTitle(PageEntity pageModel) {
        Document doc = Jsoup.parse(pageModel.getContent());
        return doc.title();
    }

    private String createSnippet(PageEntity pageModel, List<String> queryLemmas) {
        Document doc = Jsoup.parse(pageModel.getContent());
        return new Morphology(luceneMorphology).createSnippet(doc.outerHtml(), queryLemmas);
    }
}
