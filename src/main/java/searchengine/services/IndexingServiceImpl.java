package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.LemmaFinder;
import searchengine.dto.indexing.PageIndexer;
import searchengine.dto.indexing.Response.*;
import searchengine.dto.indexing.SiteParserHandler;
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
    private static boolean isStarted = false;
    private final SitesList sites;
    private List<SiteParserHandler> handlers;
    private List<Thread> threads;
    private final SiteDao siteDao = new SiteDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final PageDao pageDao = new PageDao();
    private final IndexDao indexDao = new IndexDao();
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

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

    /**
     * Поиск строки по индексированным страницам.
     *
     * @param query  искомая строка;
     * @param site   сайт, по которому осуществляется поиск (если null - по всем сайтам);
     * @param offset сдвиг от начала списка результатов (по умолчанию 0);
     * @param limit  (количество найденных страниц, которые нужно отобразить).
     * @return страницы сайтов с найденной искомой строкой.
     */
    @Override
    public Response search(String query, String site, int offset, int limit) {
        String[] errors = new String[]{
                "Сайт не проиндексирован",
                "Страницы не найдены",
                "Задан пустой поисковый запрос"
        };

        if (query.isBlank()) {
            return createFailureResponse(errors[2]);
        }

        List<searchengine.model.Site> sites = new ArrayList<>();
        if (site == null) {
            sites = siteDao.getAll().orElse(new ArrayList<>());
        } else {
            searchengine.model.Site s = new searchengine.model.Site();
            s.setUrl(site);
            s = siteDao.get(s).orElse(null);
            sites.add(s);
        }

        sites.removeIf(Objects::isNull);

        if (sites.isEmpty()) {
            return createFailureResponse(errors[0]);
        }

        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(query);
        List<String> words = new ArrayList<>();
        lemmasMap.forEach((k, v) -> words.add(k));
        List<Lemma> lemmasList = lemmaDao.getListByLemma(words.toArray()).orElse(null);

        if (lemmasList == null || lemmasList.isEmpty()) {
            return createFailureResponse(errors[1]);
        }

        HashMap<Page, Float> relevantPages = new HashMap<>();
        for (searchengine.model.Site s : sites) {
            List<Lemma> lemmas = new ArrayList<>(lemmasList
                    .stream()
                    .filter(l -> l.getSite().equals(s) && lemmasMap.containsKey(l.getLemma()))
                    .toList());

            if (lemmas.isEmpty()) {
                continue;
            }

            lemmas.sort(Lemma::compareTo);
            List<Page> pages = getPagesContainingAllLemmas(lemmas);
            relevantPages.putAll(getRelevantPages(pages, lemmas));
        }

        if (relevantPages.isEmpty()) {
            return createFailureResponse(errors[1]);
        }

        List<Map.Entry<Page, Float>> sortedByRelevance = new ArrayList<>(relevantPages.entrySet());
        sortedByRelevance.sort(Map.Entry.comparingByValue());

        SearchSuccessResponse response = new SearchSuccessResponse();
        response.setResult(true);
        response.setCount(sortedByRelevance.size());
        response.setData(getSearchData(sortedByRelevance, limit, offset, query));

        return response;
    }

    /**
     * Устанавливает значение флага запуска
     *
     * @param value значение флага
     */
    public static void setIsStarted(boolean value) {
        isStarted = value;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }

    private FailureResponse createFailureResponse(String error) {
        FailureResponse response = new FailureResponse();
        response.setResult(false);
        response.setError(error);
        return response;
    }

    private List<Page> getPagesContainingAllLemmas(List<Lemma> lemmas) {
        List<Index> indexes = indexDao.getListByLemma(lemmas.get(0)).orElse(null);
        if (indexes == null) {
            return new ArrayList<>();
        }
        List<Page> pages = pageDao.getListByIndexes(indexes).orElse(new ArrayList<>());

        for (int i = 1; i < lemmas.size(); i++) {
            List<Index> temp = indexDao.getListByLemma(lemmas.get(i)).orElse(new ArrayList<>());
            pages.removeIf(page -> temp.stream().noneMatch(e -> e.getPage().getId() == page.getId()));
        }

        return pages;
    }

    private HashMap<Page, Float> getRelevantPages(List<Page> pages, List<Lemma> lemmas) {
        HashMap<Page, Float> relevantPages = new HashMap<>();
        float maxRelevance = 0.0f;

        for (Page page : pages) {
            for (Lemma lemma : lemmas) {
                Index index = indexDao.getListByPageAndLemma(page, lemma).orElse(null);
                float rank = index.getRank();

                if (!relevantPages.containsKey(page)) {
                    relevantPages.put(page, rank);
                    maxRelevance = Math.max(maxRelevance, rank);
                } else {
                    float relevance = relevantPages.get(page) + rank;
                    maxRelevance = Math.max(maxRelevance, relevance);
                    relevantPages.put(page, relevance);
                }
            }
        }

        for (Map.Entry<Page, Float> entry : relevantPages.entrySet()) {
            relevantPages.put(entry.getKey(), entry.getValue() / maxRelevance);
        }

        return relevantPages;
    }

    private List<SearchItem> getSearchData(List<Map.Entry<Page, Float>> pages, int limit, int offset, String query) {
        List<SearchItem> items = new ArrayList<>();
        int from = pages.size() - offset - 1;
        int to = Math.max(pages.size() - limit - offset, 0);

        for (int i = from; i >= to; i--) {
            searchengine.model.Site site = pages.get(i).getKey().getSite();
            SearchItem item = new SearchItem();
            item.setSite(site.getUrl());
            item.setSiteName(site.getName());
            item.setUri(pages.get(i).getKey().getPath());
            item.setTitle(getPageTitle(pages.get(i).getKey()));
            item.setSnippet(getSnippet(pages.get(i).getKey(), query));
            item.setRelevance(pages.get(i).getValue());
            items.add(item);
        }

        return items;
    }

    private String getPageTitle(Page page) {
        String content = page.getContent();
        Document document = Jsoup.parse(content);
        return document.title().replaceAll("\\\\", "");
    }

    private String getSnippet(Page page, String query) {
        String text = Jsoup.parse(page.getContent()).text();
        String[] words = query.split("\\s");

        List<String> wordsVariations = new ArrayList<>();
        for (String word : words) {
            wordsVariations.addAll(getWordVariations(word));
        }
        wordsVariations.sort(Comparator.comparingInt(String::length).reversed());
        List<String> sentences = new ArrayList<>();

        for (String word : wordsVariations) {
            Pattern pattern = Pattern.compile("\\s.{0,30}[^А-Яа-яA-Za-z]?(" + word + ")[^А-Яа-яA-Za-z]?.{0,30}\\s");
            Matcher matcher;
            boolean isFoundInSentences = false;

            for (int i = 0; i < sentences.size(); i++) {
                matcher = pattern.matcher(sentences.get(i));
                if (matcher.find()) {
                    sentences.set(i, matcher.group().replaceAll(word, "<b>" + word + "</b>"));
                    isFoundInSentences = true;
                }
            }

            if (!isFoundInSentences) {
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String t = matcher.group().replaceAll(word, "<b>" + word + "</b>");
                    sentences.add(t.replaceAll("\\\\", ""));
                }
            }
        }

        return "... " + String.join(". ", sentences) + " ...";
    }

    private List<String> getWordVariations(String word) {
        List<String> result = new ArrayList<>();
        char firstChar = word.toUpperCase().charAt(0);
        String wordWithFirstCharUpperCase = firstChar + word.substring(1).toLowerCase();
        result.add(wordWithFirstCharUpperCase);
        result.add(word.toLowerCase());
        return result;
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