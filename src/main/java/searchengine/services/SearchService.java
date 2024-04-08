package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.dto.response.Response;

public interface SearchService {
    Response search(String query, @Nullable String site, @Nullable int offset, @Nullable int limit);
}
