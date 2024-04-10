package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.businessLogic.LemmaFinder;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.response.FailureResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SearchSuccessResponse;
import searchengine.dto.search.SearchItem;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchServiceImpl implements SearchService {
    private static final String[] ERRORS = new String[]{
            "Сайт не проиндексирован",
            "Страницы не найдены",
            "Задан пустой поисковый запрос"
    };

    private final SiteDao siteDao = new SiteDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final PageDao pageDao = new PageDao();
    private final IndexDao indexDao = new IndexDao();
    private final LemmaFinder lemmaFinder = LemmaFinder.getInstance();

    /**
     * Поиск строки по индексированным страницам.
     *
     * @param query  искомая строка;
     * @param site   сайт, по которому осуществляется поиск (если null - по всем сайтам);
     * @param offset сдвиг от начала списка результатов (по умолчанию 0);
     * @param limit  количество найденных страниц, которые нужно отобразить.
     * @return страницы сайтов с найденной искомой строкой.
     */
    @Override
    public Response search(String query, String site, int offset, int limit) {
        if (query.isBlank())
            return createFailureResponse(ERRORS[2]);

        List<Site> sites = getSites(site);

        if (sites.isEmpty())
            return createFailureResponse(ERRORS[0]);

        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(query);
        HashMap<Page, Float> relevantPages = getPages(sites, lemmasMap);

        if (relevantPages.isEmpty())
            return createFailureResponse(ERRORS[1]);

        List<Map.Entry<Page, Float>> sortedByRelevance = new ArrayList<>(relevantPages.entrySet());
        sortedByRelevance.sort(Map.Entry.comparingByValue());

        return createSearchSuccessResponse(
                sortedByRelevance.size(),
                getSearchData(sortedByRelevance, limit, offset, query)
        );
    }

    private List<Site> getSites(String site) {
        List<Site> sites = new ArrayList<>();

        if (site == null) {
            sites = siteDao.getAll().orElse(new ArrayList<>());
        } else {
            searchengine.model.Site s = new searchengine.model.Site();
            s.setUrl(site);
            s = siteDao.get(s).orElse(null);
            sites.add(s);
        }

        sites.removeIf(Objects::isNull);

        return sites;
    }

    private HashMap<Page, Float> getPages(List<Site> sites, HashMap<String, Integer> lemmasMap) {
        HashMap<Page, Float> relevantPages = new HashMap<>();

        for (searchengine.model.Site site : sites) {
            List<String> words = new ArrayList<>();
            lemmasMap.forEach((k, v) -> words.add(k));
            List<Lemma> lemmas = lemmaDao.getLemmasByListAndSite(words.toArray(), site).orElse(null);

            if (lemmas == null || lemmas.isEmpty() || lemmas.size() != lemmasMap.size())
                continue;

            lemmas.sort(Lemma::compareTo);
            List<Page> pages = getPagesContainingAllLemmas(lemmas);
            relevantPages.putAll(getRelevantPages(pages, lemmas));
        }

        return relevantPages;
    }

    private FailureResponse createFailureResponse(String error) {
        FailureResponse response = new FailureResponse();
        response.setResult(false);
        response.setError(error);
        return response;
    }

    private SearchSuccessResponse createSearchSuccessResponse(int count, List<SearchItem> data) {
        SearchSuccessResponse response = new SearchSuccessResponse();
        response.setResult(true);
        response.setCount(count);
        response.setData(data);
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
            Pattern pattern = Pattern.compile("\\b.{0,50}(" + word + ").{0,50}\\b");
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
