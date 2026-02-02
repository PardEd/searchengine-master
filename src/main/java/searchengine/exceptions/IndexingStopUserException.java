package searchengine.exceptions;

public class IndexingStopUserException extends RuntimeException {

    public IndexingStopUserException() {
        super("Индексация остановлена пользователем");
    }
}
