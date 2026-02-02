package searchengine.services.components;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.exceptions.IndexingConnectionSiteException;
import searchengine.exceptions.IndexingStopUserException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

@Slf4j
@RequiredArgsConstructor
public class PageParser extends RecursiveAction {

    private final String path;

    private final SiteEntity siteEntity;

    private final SiteParser siteParser;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;
    private boolean isSingle;

    private final Set<String> checkUrls;
    private static final int MEDIUMTEXT_SIZE = 16_777_215;
    private static final String ROOT_ERROR_MESSAGE = "Ошибка индексации: главная страница сайта недоступна";
    private static final String GENERAL_ERROR_MESSAGE = "Ошибка индексации: ";

    public PageParser(String path, SiteEntity siteEntity, SiteParser siteParser,
        SiteRepository siteRepository, PageRepository pageRepository,
        LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.path = path;
        this.siteEntity = siteEntity;
        this.siteParser = siteParser;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.checkUrls = new TreeSet<>();
    }

    public void setSingleParsing() {
        isSingle = true;
    }

    @Override
    protected void compute() {
        log.info("Start {} " + " {}", siteEntity.getUrl(), LocalDateTime.now());
        PageEntity pageEntity;
        synchronized (siteEntity) {
            if (!siteParser.isIndexing()) {
                throw new IndexingStopUserException();
            }
            pageEntity = parsePage();
        }
        if (pageEntity == null) {
            return;
        }
        PageIndexer pageIndexer = new PageIndexer(siteEntity, siteParser, lemmaRepository,
            indexRepository);
        pageIndexer.indexPage(pageEntity);
        if (isSingle) {
            return;
        }
        Set<String> childUrls = findChildUrls(pageEntity);
        ForkJoinTask.invokeAll(createSubtasks(childUrls));
        log.info("Stop {}" + " {} ", siteEntity.getUrl(), LocalDateTime.now());
    }

    private PageEntity parsePage() {
        if (pageRepository.findPageIdByUrlSiteId(path, siteEntity.getId()) != null) {
            return null;
        }
        Document doc = connectPage();
        if (doc == null || doc.outerHtml().length() >= MEDIUMTEXT_SIZE) {
            return null;
        }
        PageEntity pageEntity = createPageEntity(path, siteEntity,
            doc.connection().response().statusCode(), doc.outerHtml());

        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteEntity);

        if (!isSingle) {
            try {
                Thread.sleep(siteParser.getConfig().getTimeout());
            } catch (InterruptedException ex) {
                throw new IndexingStopUserException();
            }
        }
        return pageEntity;
    }

    private Document connectPage() {
        try {
            return Jsoup.connect(siteEntity.getUrl() + path)
                .userAgent(siteParser.getConfig().getUserAgent())
                .referrer(siteParser.getConfig().getReferrer())
                .get();
        } catch (HttpStatusException ex) {
            if (isSingle) {
                siteParser.stopPoolIndexing();
                throw new IndexingConnectionSiteException(ex.getStatusCode(), ex.getMessage());
            }
            createPageEntity(path, siteEntity, ex.getStatusCode(), ex.getMessage());
            if (path.equals("/")) {
                setErrorStatusSite(siteEntity, ROOT_ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            setErrorStatusSite(siteEntity, GENERAL_ERROR_MESSAGE + ex.getMessage());
            log.warn("{}{} {}", siteEntity.getUrl(), path, ex.getMessage());
        }
        return null;
    }

    private PageEntity createPageEntity(String url, SiteEntity siteModel, int statusCode,
        String context) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPath(url);
        pageEntity.setSiteId(siteModel);
        pageEntity.setCode(statusCode);
        pageEntity.setContent(context);
        pageRepository.saveAndFlush(pageEntity);
        if (!isSingle) {
            log.info("{}{} добавлена", siteModel.getUrl(), path);
        }
        return pageEntity;
    }

    private Set<String> findChildUrls(PageEntity pageModel) {
        Document doc = Jsoup.parse(pageModel.getContent());
        Set<String> childUrls = new HashSet<>();
        Elements elements = doc.select("a[href^=/]");
        elements.forEach(element -> childUrls.add(element.attr("href")));
        return childUrls;
    }

    private List<PageParser> createSubtasks(Set<String> childUrls) {
        List<PageParser> subtasks = new ArrayList<>();
        String subRootPath = getSubRootSite(siteEntity.getUrl());
        for (String url : childUrls) {
            url = url.toLowerCase();
            if (!subRootPath.isEmpty()) {
                if (!url.contains(subRootPath)) {
                    continue;
                } else {
                    url = url.replaceAll(subRootPath, "");
                }
            }
            if (url.equals(path)
                || checkUrls.contains(url)
                || (url.contains("http") && !url.contains(siteEntity.getUrl()))
                || url.contains("/sort/")
                || url.matches("^.*\\.(?!html)([a-z]+)$")
                || url.contains("%")
                || url.contains("#")
                || url.contains("?")) {
                continue;
            }
            checkUrls.add(url);
            subtasks.add(new PageParser(url, siteEntity, siteParser, siteRepository,
                pageRepository, lemmaRepository, indexRepository, checkUrls));
        }
        return subtasks;
    }

    private String getSubRootSite(String url) {
        String[] urls = url.split("/");
        String subRootSite = "";
        if (urls.length > 3) {
            for (int i = 3; i < urls.length; i++) {
                subRootSite = subRootSite.concat("/").concat(urls[i]);
            }
        }
        return subRootSite;
    }

    private void setErrorStatusSite(SiteEntity siteEntity, String message) {
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError(message);
        siteRepository.saveAndFlush(siteEntity);
    }
}