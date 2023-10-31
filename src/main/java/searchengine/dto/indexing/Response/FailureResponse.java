package searchengine.dto.indexing.Response;

import lombok.Data;

@Data
public class FailureResponse extends Response {
    private String error;
}
