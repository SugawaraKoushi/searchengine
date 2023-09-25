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
import searchengine.dto.indexing.SiteParserHandler;
import searchengine.dto.indexing.search.SearchItem;
import searchengine.dto.indexing.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Status;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Pattern URL_PATTERN = Pattern.compile("(?<root>https?://[^/]+)?(?<path>.+)");
    private boolean isStarted = false;
    private final SitesList sites;
    private List<SiteParserHandler> handlers;
    private List<Thread> threads;
    private final SiteDao siteDao = new SiteDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final PageDao pageDao = new PageDao();
    private final IndexDao indexDao = new IndexDao();
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

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

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        String[] errors = new String[]{"Сайт не проиндексирован", "Страницы не найдены"};
        SearchResponse response = new SearchResponse();
        searchengine.model.Site s = new searchengine.model.Site();
        s.setUrl(site);
        s = siteDao.get(s).orElse(null);

        // Сайт не проиндексирован
        if (s == null) {
            response.setResult(false);
            //response.setError(errors[0]);
            return response;
        }

        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(query);
        List<String> words = new ArrayList<>();
        lemmasMap.forEach((k, v) -> words.add(k));
        List<Lemma> lemmasList = lemmaDao.getListByLemma(words.toArray()).orElse(null);

        // Нет страниц с такими леммами
        if (lemmasList == null || lemmasList.isEmpty()) {
            response.setResult(false);
            //response.setError(errors[1]);
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
                    response.setResult(false);
                    //response.setError(errors[1]);
                    return response;
                }

                pagesList = pageDao.getListByIndexes(indexList).orElse(null);
            } else {
                List<Index> temp = indexDao.getListByLemma(lemmasList.get(i)).orElse(new ArrayList<>());
                pagesList.removeIf(page -> temp.stream().noneMatch(e -> e.getPage().getId() == page.getId()));
            }
        }

        if (pagesList == null || pagesList.isEmpty()) {
            response.setResult(false);
            //response.setError(errors[1]);
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

        response.setResult(true);
        response.setCount(sortedByRelevance.size());
        response.setData(getSearchData(sortedByRelevance, limit, s, query));

        return response;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }

    private List<SearchItem> getSearchData(List<Map.Entry<Page, Float>> sortedPages, int limit, searchengine.model.Site site, String query) {
        List<SearchItem> items = new ArrayList<>();
        limit = Math.min(limit, sortedPages.size());

        for (int i = sortedPages.size() - 1; i >= sortedPages.size() - limit; i--) {
            SearchItem item = new SearchItem();
            item.setSite(site.getUrl());
            item.setSiteName(site.getName());
            item.setUri(sortedPages.get(i).getKey().getPath());
            item.setTitle(getPageTitle(sortedPages.get(i).getKey()));
            item.setSnippet(getSnippet(sortedPages.get(i).getKey(), query));
            item.setRelevance(sortedPages.get(i).getValue());
            items.add(item);
        }

        return items;
    }

    private String getPageTitle(Page page) {
        String content = page.getContent();
        Document document = Jsoup.parse(content);
        return document.title();
    }

    private String getSnippet(Page page, String query) {
        String text = Jsoup.parse(page.getContent()).text();
        String[] words = query.split("\\s");
        Arrays.sort(words, Comparator.comparingInt(String::length).reversed());
        List<String> sentences = new ArrayList<>();

        for (String word : words) {
            Pattern pattern = Pattern.compile("\\b.{0,100}\\s(" + word + ")\\s.{0,100}\\b");
            Matcher matcher;
            boolean isFoundInSentences = false;

            for (int i = 0; i < sentences.size(); i++) {
                matcher = pattern.matcher(sentences.get(i));
                if (matcher.find()) {
                    sentences.set(i, matcher.group().replaceAll("\\s" + word + "\\s", " <b>" + word + "</b> "));
                    isFoundInSentences = true;
                }
            }

            if (!isFoundInSentences) {
                matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String t = matcher.group().replaceAll("\\s" + word + "\\s", " <b>" + word + "</b> ");
                    sentences.add(t);
                }
            }
        }

        return "... " + String.join(".\n", sentences) + " ...";
    }
}