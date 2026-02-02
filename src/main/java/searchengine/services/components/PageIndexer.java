package searchengine.services.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import searchengine.exceptions.IndexingStopUserException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

public class PageIndexer {

    private final SiteEntity siteEntity;
    private final SiteParser siteParser;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final List<IndexEntity> newIndexEntitySavePack = new ArrayList<>();
    private final List<LemmaEntity> newLemmaEntitySavePack = new ArrayList<>();
    private final List<Long> updateLemmaEntityPack = new ArrayList<>();
    private static final int BATCH_SIZE = 500;

    public PageIndexer(SiteEntity siteEntity, SiteParser siteParser,
        LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteEntity = siteEntity;
        this.siteParser = siteParser;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    /**
     * Метод извлекает из текстов страниц слова, преобразовывать их в леммы, считает количество
     * вхождений каждой леммы в текст и сохранять эту информацию в базу данных.
     *
     * @param pageModel страничка сайта которая будет обрабатываться
     * @throws IndexingStopUserException если прервано пользователем
     */
    public void indexPage(PageEntity pageModel) {
        Morphology morphology = new Morphology(siteParser.getLuceneMorphology());
        HashMap<String, Integer> lemmasCount = morphology.collectLemmas(pageModel.getContent());
        synchronized (siteEntity) {
            if (!siteParser.isIndexing()) {
                throw new IndexingStopUserException();
            }
            createLemmaAndIndexModels(lemmasCount, pageModel);
            saveLemmaPack();
            saveIndexPack();
        }
    }

    private void createLemmaAndIndexModels(HashMap<String, Integer> lemmasCount,
        PageEntity pageEntity) {
        for (String lemma : lemmasCount.keySet()) {
            Long lemmaId = lemmaRepository.findIdLemmaByNameAndSiteId(lemma, siteEntity.getId());
            LemmaEntity lemmaEntity;
            if (lemmaId == null) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteId(siteEntity);
                lemmaEntity.setLemma(lemma);
                lemmaEntity.setFrequency(1);
                newLemmaEntitySavePack.add(lemmaEntity);
            } else {
                lemmaEntity = lemmaRepository.findById(lemmaId)
                    .orElseThrow(NullPointerException::new);
                updateLemmaEntityPack.add(lemmaId);
            }
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPageId(pageEntity);
            indexEntity.setLemmaId(lemmaEntity);
            indexEntity.setRating(Float.valueOf(lemmasCount.get(lemma).toString()));
            newIndexEntitySavePack.add(indexEntity);
        }
    }

    private void saveLemmaPack() {
        if (!siteParser.isIndexing()) {
            throw new IndexingStopUserException();
        }
        if (!newLemmaEntitySavePack.isEmpty()) {
            if (newLemmaEntitySavePack.size() >= BATCH_SIZE) {
                for (int i = 0; i < newLemmaEntitySavePack.size(); i += BATCH_SIZE) {
                    List<LemmaEntity> batchLemmaModels = newLemmaEntitySavePack.subList(i,
                        Math.min(i + BATCH_SIZE, newLemmaEntitySavePack.size()));
                    lemmaRepository.saveAllAndFlush(batchLemmaModels);
                }
            } else {
                lemmaRepository.saveAllAndFlush(newLemmaEntitySavePack);
            }
        }
        if (!updateLemmaEntityPack.isEmpty()) {
            lemmaRepository.incrementFrequency(updateLemmaEntityPack);
        }
    }

    private void saveIndexPack() {
        if (!siteParser.isIndexing()) {
            throw new IndexingStopUserException();
        }
        if (newIndexEntitySavePack.size() >= BATCH_SIZE) {
            for (int i = 0; i < newIndexEntitySavePack.size(); i += BATCH_SIZE) {
                List<IndexEntity> batchLemmaModels = newIndexEntitySavePack.subList(i,
                    Math.min(i + BATCH_SIZE, newIndexEntitySavePack.size()));
                indexRepository.saveAll(batchLemmaModels);
            }
        } else {
            indexRepository.saveAll(newIndexEntitySavePack);
        }
    }
}
