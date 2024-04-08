package searchengine.dto.response;

import lombok.Data;
import searchengine.dto.search.SearchItem;

import java.util.List;

@Data
public class SearchSuccessResponse extends Response {
    private int count;
    private List<SearchItem> data;
}
