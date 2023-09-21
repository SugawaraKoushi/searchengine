package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
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

//        isStarted = false;

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
    public int search(String query, String site, int offset, int limit) {
        searchengine.model.Site s = new searchengine.model.Site();
        s.setUrl(site);
        s = siteDao.get(s).orElse(null);

        if (s == null) {
            return 0;
        }

        HashMap<String, Integer> lemmasMap = lemmaFinder.getLemmas(query);
        List<Lemma> lemmasList = lemmaDao.getAll().orElse(null);

        if (lemmasList == null || lemmasList.isEmpty()) {
            return -1;
        }

        searchengine.model.Site tempS = s;
        lemmasList = new ArrayList<>(lemmasList
                .stream()
                .filter(l -> l.getSite().equals(tempS) && lemmasMap.containsKey(l.getLemma()))
                .toList());

        lemmasList.sort(Lemma::compareTo);

        List<Page> pagesList = new ArrayList<>();
        List<Index> indexList = new ArrayList<>();
        List<Lemma> finalLemmasList = lemmasList;

        for (int i = 0; i < lemmasList.size(); i++) {

            if (i == 0) {
                indexList = indexDao.getListByLemma(lemmasList.get(i)).orElse(null);

                if (indexList == null || indexList.isEmpty()) {
                    return -1;
                }
            } else {
                List<Index> temp = indexDao.getListByLemma(lemmasList.get(i)).orElse(null);
                indexList.removeIf(index -> temp.stream().noneMatch(e -> e.getPage().equals(index.getPage())));
            }
        }

        for (Index index : indexList) {
            pageDao.get(index.getPage().getId()).ifPresent(pagesList::add);
        }

        return 1;
    }

    private void createSiteParserHandlers() {
        for (searchengine.config.Site site : sites.getSites()) {
            handlers.add(new SiteParserHandler(site));
        }
    }
}