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
import searchengine.dto.indexing.Response.Response;
import searchengine.dto.indexing.Response.SearchFailureResponse;
import searchengine.dto.indexing.SiteParserHandler;
import searchengine.dto.indexing.Response.SearchItem;
import searchengine.dto.indexing.Response.SearchSuccessResponse;
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
    public int startIndexing() {
        handlers = new ArrayList<>();
        threads = new ArrayList<>();

        if (isStarted)
            return -1;

        isStarted = true;
        createSiteParserHandlers();

        for (SiteParserHandler handler : handlers) {
            threads.add(new Thread(handler));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        return 0;
    }

    /**
     * Останавливает индексацю всех сайтов.
     *
     * @return Успешность остановки.
     */
    @Override
    public int stopIndexing() {
        if (!isStarted)
            return -1;

        for (SiteParserHandler handler : handlers) {
            handler.stopParsing();
        }

        for (Thread thread : threads) {
            thread.interrupt();
        }

        isStarted = false;
        return 0;
    }

    /**
     * Запускает индексацю отдельной страницы.
     *
     * @param url URL страницы.
     * @return Успешность индексации страницы.
     */
    @Override
    public int indexPage(String url) {
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
                s.setName(site.getName());
                s.setStatus(Status.INDEXING);
                s.setStatusTime(new Date());
                s.setUrl(root);

                Page p = new Page();
                p.setPath(path);
                p.setSite(s);

                PageIndexer indexer = new PageIndexer(s, p);
                indexer.index();

                return 0;
            }
        }

        return -1;
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
        String[] errors = new String[]{"Сайт не проиндексирован", "Страницы не найдены"};
        searchengine.model.Site s = new searchengine.model.Site();
        s.setUrl(site);
        s = siteDao.get(s).orElse(null);

        // Сайт не проиндексирован
        if (s == null) {
            SearchFailureResponse response = new SearchFailureResponse();
            response.setResult(false);
            response.setError(errors[0]);
            return response;
        }

        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(query);
        List<String> words = new ArrayList<>();
        lemmasMap.forEach((k, v) -> words.add(k));
        List<Lemma> lemmasList = lemmaDao.getListByLemma(words.toArray()).orElse(null);

        // Нет страниц с такими леммами
        if (lemmasList == null || lemmasList.isEmpty()) {
            SearchFailureResponse response = new SearchFailureResponse();
            response.setResult(false);
            response.setError(errors[1]);
            return response;
        }

        searchengine.model.Site tempS = s;
        lemmasList = new ArrayList<>(lemmasList
                .stream()
                .filter(l -> l.getSite().equals(tempS) && lemmasMap.containsKey(l.getLemma()))
                .toList());

        lemmasList.sort(Lemma::compareTo);

        List<Page> pagesList = new ArrayList<>();
        List<Index> indexList;

        for (int i = 0; i < lemmasList.size(); i++) {

            if (i == 0) {
                indexList = indexDao.getListByLemma(lemmasList.get(i)).orElse(null);
                // Не найдено индексов с искомыми леммами
                if (indexList == null || indexList.isEmpty()) {
                    SearchFailureResponse response = new SearchFailureResponse();
                    response.setResult(false);
                    response.setError(errors[1]);
                    return response;
                }

                pagesList = pageDao.getListByIndexes(indexList).orElse(null);
            } else {
                List<Index> temp = indexDao.getListByLemma(lemmasList.get(i)).orElse(new ArrayList<>());
                pagesList.removeIf(page -> temp.stream().noneMatch(e -> e.getPage().getId() == page.getId()));
            }
        }

        if (pagesList == null || pagesList.isEmpty()) {
            SearchFailureResponse response = new SearchFailureResponse();
            response.setResult(false);
            response.setError(errors[1]);
            return response;
        }

        HashMap<Page, Float> foundPages = new HashMap<>();
        float maxRelevance = 0.0f;
        for (Page page : pagesList) {
            for (Lemma lemma : lemmasList) {
                Index index = indexDao.getListByPageAndLemma(page, lemma).orElse(null);
                float rank = index.getRank();

                if (!foundPages.containsKey(page)) {
                    foundPages.put(page, rank);
                } else {
                    float relevance = foundPages.get(page) + rank;
                    maxRelevance = Math.max(maxRelevance, relevance);
                    foundPages.put(page, relevance);
                }
            }
        }

        for (Map.Entry<Page, Float> entry : foundPages.entrySet()) {
            foundPages.put(entry.getKey(), entry.getValue() / maxRelevance);
        }

        List<Map.Entry<Page, Float>> sortedByRelevance = new ArrayList<>(foundPages.entrySet());
        sortedByRelevance.sort(Map.Entry.comparingByValue());

        SearchSuccessResponse response = new SearchSuccessResponse();
        response.setResult(true);
        response.setCount(sortedByRelevance.size());
        response.setData(getSearchData(sortedByRelevance, limit, offset, s, query));

        return response;
    }

    public static void setIsStarted(boolean value) {
        isStarted = value;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }

    private List<SearchItem> getSearchData(List<Map.Entry<Page, Float>> pages, int limit, int offset, searchengine.model.Site site, String query) {
        List<SearchItem> items = new ArrayList<>();
        int from = pages.size() - offset - 1;
        int to = Math.max(pages.size() - limit - offset, 0);

        for (int i = from; i >= to; i--) {
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
}