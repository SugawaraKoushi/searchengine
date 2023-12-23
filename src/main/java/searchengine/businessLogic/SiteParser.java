package searchengine.businessLogic;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Возвращает все содержащиеся страницы на текущей странице
     */
    @Override
    protected HashSet<Page> compute() {
        HashSet<Page> result = new HashSet<>();
        List<SiteParser> tasks = new ArrayList<>();
        page.setSite(site);
        HashSet<Page> pages = handle(page);
        updateSiteStatusTime();

        if (pages == null)
            return null;

        for (Page p : pages) {
            SiteParser task = new SiteParser(site, p.getPath());
            task.fork();
            tasks.add(task);
        }

        result.add(page);

        for (SiteParser task : tasks) {
            result.add(task.page);

            HashSet<Page> taskResult = task.join();
            if (taskResult != null) {
                result.addAll(taskResult);
            }
        }

        return result;
    }

    /**
     * Парсинг страницы
     * @param page
     */
    public static void parsePage(Page page) {
        try {
            Connection.Response response = Jsoup.connect(getRoot(page.getSite().getUrl()) + page.getPath())
                    .userAgent("BobTheSearcherBot")
                    .referrer("http://www.google.com")
                    .timeout(60000)
                    .execute();
            TimeUnit.MILLISECONDS.sleep(500);

            Document doc = response.parse();
            String content = doc.html();

            page.setContent(content);
            page.setCode(response.statusCode());
        } catch (Exception e) {
            page.setCode(getErrorResponseCode(e.getMessage()));
            page.getSite().setLastError(e.getMessage());
        }
    }

    private HashSet<Page> handle(Page page) {
        if (stop) {
            stop();
            return null;
        }

        HashSet<Page> result = new HashSet<>();
        parsePage(page);

        if (StringUtil.isBlank(page.getContent())) {
            return null;
        }

        HashSet<String> hrefs = getValidHrefs(page);
        for (String href : hrefs) {
            Page p = new Page();
            p.setPath(href);
            result.add(p);
        }

        return result;
    }

    public void setStop(boolean value) {
        stop = value;
    }

    private boolean isValidPath(String path, Page page) {
        return !path.equals(page.getPath()) && path.startsWith(page.getPath()) && !path.contains("#");
    }

    private static int getErrorResponseCode(String httpErrorMessage) {
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

    private static String getRoot(String url) {
        Pattern urlPattern = Pattern.compile("(?<root>https?://[^/]+)(?<path>.+)?");
        Matcher matcher = urlPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group("root");
        }

        return url;
    }

    private void stop() {
        logger.info("User stop the parsing");
        updateSiteLastError("Индексация остановлена пользователем");
        updateSiteStatus(Status.FAILED);
    }

    private HashSet<String> getValidHrefs(Page page) {
        Document doc = Jsoup.parse(page.getContent());
        Elements elements = doc.select("a");
        HashSet<String> hrefs = new HashSet<>();
        elements.forEach(element -> hrefs.add(element.attr("href")));
        hrefs.removeIf(href -> !this.isValidPath(href, page));
        return hrefs;
    }
}
