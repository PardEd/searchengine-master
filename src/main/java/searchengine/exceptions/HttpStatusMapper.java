package searchengine.exceptions;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

@Component
public class HttpStatusMapper {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public HttpStatusMapper(MessageSource messageSource, LocaleResolver localeResolver) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    public HttpStatus getStatusHttp(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public String getMessage(int code) {
        Locale locale = localeResolver.resolveLocale(null);
        String messageKey = "http.status." + code;

        try {
            return messageSource.getMessage(messageKey, null, messageSource.getMessage("http.status.default", null, locale), locale);
        } catch (NoSuchMessageException e) {
            return messageSource.getMessage("http.status.default", null, locale);
        }
    }
}
