package searchengine.exceptions;

public class SearchNoReadyIndexException extends RuntimeException {
    public SearchNoReadyIndexException() {
        super("Ещё нет готового индекса");
    }
}