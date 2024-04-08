package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.businessLogic.LemmaFinder;
import searchengine.businessLogic.PageIndexer;
import searchengine.dto.search.SearchItem;
import searchengine.businessLogic.SiteParserHandler;
import searchengine.dto.response.FailureResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SearchSuccessResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Status;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<root>https?://[^/]+)?(?<path>.+)");

    @Setter
    private static boolean isStarted = false;
    private final SitesList sites;
    private List<SiteParserHandler> handlers;
    private List<Thread> threads;
    private final SiteDao siteDao = new SiteDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final IndexDao indexDao = new IndexDao();

    /**
     * Запускает полную индесацию всех сайтов.
     *
     * @return Успешность запуска.
     */
    @Override
    public Response startIndexing() {
        handlers = new ArrayList<>();
        threads = new ArrayList<>();

        if (isStarted) {
            FailureResponse response = new FailureResponse();
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }

        isStarted = true;
        createSiteParserHandlers();

        for (SiteParserHandler handler : handlers) {
            threads.add(new Thread(handler));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        Response response = new Response();
        response.setResult(true);

        return response;
    }

    /**
     * Останавливает индексацю всех сайтов.
     *
     * @return Успешность остановки.
     */
    @Override
    public Response stopIndexing() {
        if (!isStarted) {
            FailureResponse response = new FailureResponse();
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }

        for (SiteParserHandler handler : handlers) {
            handler.stopParsing();
        }

        for (Thread thread : threads) {
            thread.interrupt();
        }

        isStarted = false;

        Response response = new Response();
        response.setResult(true);

        return response;
    }

    /**
     * Запускает индексацю отдельной страницы.
     *
     * @param url URL страницы.
     * @return Успешность индексации страницы.
     */
    @Override
    public Response indexPage(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        Site site = new Site();

        if (matcher.find()) {
            List<Site> sortedSites = sites.getSites().stream().sorted().toList();
            site.setUrl(matcher.group("root"));
            int index = Collections.binarySearch(sortedSites, site);

            if (index > -1) {
                site = sortedSites.get(index);
                String root = matcher.group("root");
                String path = matcher.group("path");


                searchengine.model.Site s = new searchengine.model.Site();
                s.setUrl(root);
                s = siteDao.get(s).orElse(new searchengine.model.Site());
                s.setName(site.getName());
                s.setUrl(root);
                s.setStatus(Status.INDEXING);
                s.setStatusTime(new Date());

                Page p = new Page();
                p.setPath(path);
                p.setSite(s);

                PageIndexer indexer = new PageIndexer(s, p);
                indexer.index();

                saveAndClearCurrentSiteLemmas(PageIndexer.getLemmas(), s);
                saveAndClearCurrentSiteIndexes(PageIndexer.getIndexes(), s);

                Response response = new Response();
                response.setResult(true);

                return response;
            }
        }

        FailureResponse response = new FailureResponse();
        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        return response;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }

    private void saveAndClearCurrentSiteLemmas(Map<String, Lemma> lemmasMap, searchengine.model.Site site) {
        Set<Lemma> lemmasSet = new HashSet<>();
        for (Lemma lemma : lemmasMap.values()) {
            if (lemma.getSite().getId() == site.getId()) {
                lemmasSet.add(lemma);
            }
        }

        lemmaDao.saveOrUpdateBatch(lemmasSet);

        for (Map.Entry<String, Lemma> entry : PageIndexer.getLemmas().entrySet()) {
            if (lemmasSet.contains(entry.getValue())) {
                PageIndexer.getLemmas().remove(entry.getKey());
            }
        }
    }

    private void saveAndClearCurrentSiteIndexes(Collection<Index> indexes, searchengine.model.Site site) {
        List<Index> indexesSet = new ArrayList<>();

        for (Index index : indexes) {
            if (index.getPage().getSite().getId() == site.getId()) {
                indexesSet.add(index);
            }
        }

        indexDao.saveOrUpdateBatch(indexesSet);
        indexes.removeAll(indexesSet);
    }
}