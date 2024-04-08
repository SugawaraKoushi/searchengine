package searchengine.dto.response;

import lombok.Data;

@Data
public class FailureResponse extends Response {
    private String error;
}
