package searchengine.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexingNotStartedException extends RuntimeException {

    public IndexingNotStartedException() {
        super("Индексация не запущена");
        log.warn(getMessage());
    }

    /**
     * Конструктор с дополнительной информацией и причиной
     *
     * @param message дополнительная информация
     * @param cause причина возникновения исключения
     */
    public IndexingNotStartedException(String message, Throwable cause) {
        super("Индексация не запущена. " + message, cause);
        log.warn(getMessage(), cause);
    }
}
