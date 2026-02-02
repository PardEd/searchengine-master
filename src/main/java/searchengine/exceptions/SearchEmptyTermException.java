package searchengine.exceptions;

public class SearchEmptyTermException extends RuntimeException {

    public SearchEmptyTermException() {
        super("Задан пустой поисковый запрос");
    }
}
