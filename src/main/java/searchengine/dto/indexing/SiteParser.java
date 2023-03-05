package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.dao.Dao;
import searchengine.dao.SiteDao;
import searchengine.model.Site;
import searchengine.model.Page;
import searchengine.model.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;


public class SiteParser extends RecursiveTask<HashSet<Page>> {
    private static boolean stop = false;
    private final Dao<Site> siteDao = new SiteDao();
    private final Logger logger = LoggerFactory.getLogger(SiteParser.class);
    private Site site;
    private final Page page = new Page();

    public SiteParser(Site site, String url) {
        this.site = site;
        this.page.setPath(url);
    }

    public SiteParser(Site site) {
        this(site, "/");
    }

    @Override
    protected HashSet<Page> compute() {
        HashSet<Page> result = new HashSet<>();         // Все страницы с сайта
        List<SiteParser> tasks = new ArrayList<>();     // Таски
        logger.info("Start parsing: " + site.getUrl() + page.getPath());
        HashSet<Page> pages = handle(page);             // Страницы из текущей

        page.setSite(site);
        updateSiteStatusTime();

        if (pages == null)
            return null;

        // Создаем таски
        for (Page p : pages) {
            SiteParser task = new SiteParser(site, p.getPath());
            task.fork();
            tasks.add(task);
        }

        result.add(page);

        for (SiteParser task : tasks) {
            result.add(task.page);
            HashSet<Page> taskResult = task.join();
            if (taskResult != null)
                result.addAll(taskResult);
        }

        return result;
    }

    private HashSet<Page> handle(Page page) {
        if (stop) {
            updateSiteLastError("Индексация остановлена пользователем");
            updateSiteStatus(Status.FAILED);
            return null;
        }

        HashSet<Page> result = new HashSet<>();

        try {
            Connection.Response response = Jsoup.connect(site.getUrl() + page.getPath())
                    .userAgent("BobTheSearcherBot")
                    .referrer("http://www.google.com")
                    .timeout(60000)
                    .execute();

            TimeUnit.MILLISECONDS.sleep(500);

            Document doc = response.parse();

            // Код ответа
            page.setCode(response.statusCode());

            // Код страницы
            String content = doc.toString();
            content = content.replaceAll("'", "\\\\'");
            content = content.replaceAll("\"", "\\\\\"");
            page.setContent(content);

            // Получаем ссылки со страницы, удаляя ненужное
            Elements elements = doc.select("a");
            HashSet<String> hrefs = new HashSet<>();
            elements.forEach(element -> hrefs.add(element.attr("href")));
            hrefs.removeIf(href -> !this.isValidPath(href));

            // Создаем объекты с нужными path
            for (String href : hrefs) {
                Page p = new Page();
                p.setPath(href);
                result.add(p);
            }

        } catch (Exception e) {
            page.setCode(getErrorResponseCode(e.getMessage()));
            site.setLastError(e.getMessage());
            return null;
        }

        return result;
    }

    private boolean isValidPath(String path) {
        return !path.equals(page.getPath()) && path.startsWith(page.getPath()) && !path.contains("#");
    }

    private int getErrorResponseCode(String httpErrorMessage) {
        if (!httpErrorMessage.toLowerCase().contains("status")) {
            return 408;
        }

        int start = httpErrorMessage.indexOf('=') + 1;
        int end = httpErrorMessage.indexOf(',');
        return Integer.parseInt(httpErrorMessage.substring(start, end));
    }

    private void updateSiteStatusTime() {
        site.setStatusTime(new Date(System.currentTimeMillis()));
        siteDao.update(site);
    }

    private void updateSiteStatus(Status status) {
        site.setStatus(status);
        siteDao.update(site);
    }

    private void updateSiteLastError(String error) {
        site.setLastError(error);
        siteDao.update(site);
    }

    public void stop() {
        stop = true;
    }
}
