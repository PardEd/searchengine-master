package searchengine.exceptions;

import org.springframework.http.HttpStatus;

public class IndexingConnectionSiteException extends RuntimeException {

    final int code;
    HttpStatusMapper httpStatusMapper;

    public IndexingConnectionSiteException(int code, String message) {
        super("Ошибка индексации: " + getTextParseError(code, message));
        this.code = code;
    }

    static String getTextParseError(int code, String message) {
        return switch (code) {
            case 400 -> "400 неправильный, некорректный запрос";
            case 401 -> "401 не авторизован";
            case 403 -> "403 запрещено";
            case 404 -> "404 не найдено";
            case 405 -> "405 метод не поддерживается";
            case 500 -> "500 внутренняя ошибка сервера";
            default -> "код " + code + message;
        };
    }

    public HttpStatus getHttpStatus() {
        return httpStatusMapper.getStatusHttp(code);
    }

}
