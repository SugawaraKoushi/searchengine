package searchengine.dto.indexing.Response;

import lombok.Data;

@Data
public class SearchFailureResponse extends Response {
    private String error;
}
