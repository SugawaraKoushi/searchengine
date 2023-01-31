package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser extends RecursiveTask<HashSet<Page>> {
//    private static final Pattern URL_PATTERN = Pattern.compile("(?<root>https?://[^/]+)?(?<path>.+)");
    private static String rootUrl;
    private Page page = new Page();

    public PageParser(String url) {
        if (rootUrl == null) {
            rootUrl = url;
            page.setPath("/");
        } else {
            page.setPath(url);
        }
    }

    @Override
    protected HashSet<Page> compute() {
        HashSet<Page> result = new HashSet<>();         // Все страницы с сайта
        List<PageParser> tasks = new ArrayList<>();     // Таски
        HashSet<Page> pages = handle(page);             // Страницы из текущей

        if (pages == null)
            return null;

        // Создаем таски
        for (Page p : pages) {
            PageParser task = new PageParser(p.getPath());
            task.fork();
            tasks.add(task);
        }

        result.add(page);

        for (PageParser task : tasks) {
            result.add(task.page);
            result.addAll(task.join());
        }

        return result;
    }

    private HashSet<Page> handle(Page page) {
        HashSet<Page> result = new HashSet<>();

        try {
            Connection.Response response = Jsoup.connect(rootUrl + page.getPath())
                    .userAgent("BobTheSearcherBot")
                    .referrer("http://www.google.com")
                    .execute();

            TimeUnit.MILLISECONDS.sleep(50);

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
            e.printStackTrace();
        }

        return result;
    }

    private boolean isValidPath(String path) {
        return !path.equals(page.getPath()) && path.startsWith(page.getPath()) && !path.contains("#");
    }
}
