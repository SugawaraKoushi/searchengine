package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site implements Comparable<Site> {
    private String url;
    private String name;

    @Override
    public int compareTo(Site s) {
        return url.compareTo(s.getUrl());
    }
}
