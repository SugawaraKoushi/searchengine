package searchengine.dto.indexing.Response;

import lombok.Data;

import java.util.List;

@Data
public class SearchSuccessResponse extends Response {
    private int count;
    private List<SearchItem> data;
}
