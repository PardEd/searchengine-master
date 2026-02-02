package searchengine.exceptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DictionaryFileNotFoundException extends RuntimeException {

    /**
     * Конструктор с пользовательским сообщением и причиной
     *
     * @param message дополнительное сообщение об ошибке
     * @param cause   причина возникновения исключения
     */
    public DictionaryFileNotFoundException(String message, Throwable cause) {
        super("Ошибка индексации: файл со словарем не найден. " + message, cause);
        log.error(getMessage(), cause);
    }
}
