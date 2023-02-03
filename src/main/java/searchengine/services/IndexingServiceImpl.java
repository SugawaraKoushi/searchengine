package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.SiteParser;
import searchengine.model.Page;

import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private searchengine.config.Site site;

    @Override
    public int index() {
        return 0;
    }


    private HashSet<Page> getPagesFromSite() {
        SiteParser parser = new SiteParser(site.getUrl());
        return new ForkJoinPool().invoke(parser);
    }
}
