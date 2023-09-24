package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.LemmaDao;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;

    private final PageDao pageDao = new PageDao();
    private final LemmaDao lemmaDao = new LemmaDao();
    private final SiteDao siteDao = new SiteDao();

    @Override
    public StatisticsResponse getStatistics() {
        String notIndexed = "NOT INDEXED";
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            int pages = getPagesCount();
            int lemmas = getLemmasCount();
            item.setPages(pages);
            item.setLemmas(lemmas);

            searchengine.model.Site s = getSiteStatus(site);
            if (s == null) {
                item.setStatus(notIndexed);
            } else {
                item.setStatus(s.getStatus().name());
                item.setError(s.getLastError() == null ? "" : s.getLastError());
                item.setStatusTime(s.getStatusTime().getTime());
            }

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private int getPagesCount() {
        Optional<List<Page>> optional = pageDao.getAll();
        List<Page> pages = optional.orElse(new ArrayList<>());

        return pages.size();
    }

    private int getLemmasCount() {
        Optional<List<Lemma>> optional = lemmaDao.getAll();
        List<Lemma> lemmas = optional.orElse(new ArrayList<>());

        return lemmas.size();
    }

    private searchengine.model.Site getSiteStatus(Site site) {
        searchengine.model.Site s = new searchengine.model.Site();
        s.setUrl(site.getUrl());
        Optional<searchengine.model.Site> optional = siteDao.get(s);
        s = optional.orElse(null);

        return s;
    }
}
