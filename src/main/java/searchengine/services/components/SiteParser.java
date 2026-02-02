package searchengine.services.components;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import searchengine.config.IndexingConfiguration;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteParser {

    private final IndexingConfiguration indexingConfiguration;
    @Getter
    @Autowired
    private final LuceneMorphology luceneMorphology;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private ForkJoinPool pool;
    @Autowired
    private RetryTemplate retryTemplate;
    /**
     * Метод запускает в отдельном потоке обход все страниц сайта, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в
     * таблицу page;
     *
     * @param siteModel сайт, который нужно проиндексировать.
     */

    public void parseSite(SiteEntity siteModel) {
        long start = System.currentTimeMillis();

        if (!isIndexing()) {
            pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        }

        try {
            CompletableFuture.runAsync(() -> {
                    PageParser parser = new PageParser("/", siteModel, this,
                        siteRepository, pageRepository, lemmaRepository, indexRepository);
                    pool.invoke(parser);
                }, pool)
                .handle((res, ex) -> {
                    if (ex != null) {
                        handleError(siteModel, ex);
                    }
                    return res;
                })
                .thenRun(() -> {
                    // Логика завершения
                    pool.shutdown();
                });
        } catch (Exception e) {
            handleError(siteModel, e);
        }
    }

    private void handleError(SiteEntity siteModel, Throwable ex) {
        retryTemplate.execute(context -> {
            siteModel.setStatus(Status.FAILED);
            siteModel.setLastError(ex.getCause().getMessage());
            siteRepository.saveAndFlush(siteModel);

            log.error("Ошибка при обработке сайта: {}", siteModel.getUrl(), ex);
            sendErrorNotification(siteModel, ex);
            return null;
        }, recoveryCallback -> {
            log.error("Все попытки восстановления исчерпаны для сайта: {}", siteModel.getUrl());
            markSiteForManualCheck(siteModel);
            return null;
        });
    }

    private void recover(SiteEntity siteModel, Throwable ex) {
        log.error("Все попытки восстановления исчерпаны для сайта: {}", siteModel.getUrl());
        markSiteForManualCheck(siteModel);
    }

    private void sendErrorNotification(SiteEntity siteModel, Throwable ex) {
        // Реализация отправки уведомлений
    }

    private void markSiteForManualCheck(SiteEntity siteModel) {
        siteModel.setStatus(Status.NEEDS_CHECK);
        siteRepository.saveAndFlush(siteModel);
    }



    @Transactional
    public void deletePage(Long id) {
        PageEntity pageEntity = pageRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Страница не найдена"));

        List<IndexEntity> indexes = pageEntity.getIndexEntityList();

        Map<Long, LemmaEntity> lemmaMap = new HashMap<>();

        for (IndexEntity index : indexes) {
            LemmaEntity lemma = index.getLemmaId();
            lemmaMap.put(lemma.getId(), lemma);
        }

        lemmaMap.values().forEach(lemma -> lemma.setFrequency(lemma.getFrequency() - 1));
        lemmaRepository.saveAll(lemmaMap.values());

        pageRepository.deleteById(id);
    }

    public void stopPoolIndexing() {
        pool.shutdownNow();
    }

    public boolean isIndexing() {
        return pool != null && !pool.isShutdown();
    }

    public IndexingConfiguration getConfig() {
        return indexingConfiguration;
    }
}
