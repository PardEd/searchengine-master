package searchengine.exceptions;

public class SearchQueryTooExtensiveException extends RuntimeException {

    public SearchQueryTooExtensiveException() {
        super("Запрос не достаточно точный");
    }
}
