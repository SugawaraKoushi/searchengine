package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.Site;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;


public class SiteParser extends RecursiveTask<HashSet<Page>> {
    private Logger logger = LoggerFactory.getLogger(SiteParser.class);
    private static Site site;
    private static String rootUrl;
    private final Page page = new Page();

    public SiteParser(String url) {
        page.setPath(url);
    }

    public SiteParser(Site site) {
        this(site.getUrl());
        SiteParser.site = site;
        rootUrl = site.getUrl();
        page.setPath("/");
    }

    @Override
    protected HashSet<Page> compute() {
        HashSet<Page> result = new HashSet<>();         // Все страницы с сайта
        List<SiteParser> tasks = new ArrayList<>();     // Таски
        logger.info("Start parsing: " + site.getUrl() + page.getPath());
        HashSet<Page> pages = handle(page);             // Страницы из текущей

        if (pages == null)
            return null;

        // Создаем таски
        for (Page p : pages) {
            SiteParser task = new SiteParser(p.getPath());
            task.fork();
            tasks.add(task);
        }

        page.setSite(site);
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
        HashSet<Page> result = new HashSet<>();

        try {
            Connection.Response response = Jsoup.connect(rootUrl + page.getPath())
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
}
