package searchengine.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexingAlreadyStartedException extends RuntimeException {


    public IndexingAlreadyStartedException() {
        super("Индексация уже запущена");
        log.warn(getMessage());
    }

    /**
     * Конструктор с дополнительной информацией и причиной
     *
     * @param message дополнительная информация
     * @param cause причина возникновения исключения
     */
    public IndexingAlreadyStartedException(String message, Throwable cause) {
        super("Индексация уже запущена. " + message, cause);
        log.warn(getMessage(), cause);
    }
}
