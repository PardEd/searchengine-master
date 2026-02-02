package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.error.ErrorResponse;

@ControllerAdvice
public class DefaultExceptionHandler {

    @ExceptionHandler(DictionaryFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(DictionaryFileNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IndexingAlreadyStartedException.class)
    public ResponseEntity<ErrorResponse> handleException(IndexingAlreadyStartedException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IndexingNotStartedException.class)
    public ResponseEntity<ErrorResponse> handleException(IndexingNotStartedException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IndexingOutsideSitesException.class)
    public ResponseEntity<ErrorResponse> handleException(IndexingOutsideSitesException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingConnectionSiteException.class)
    public ResponseEntity<ErrorResponse> handleException(IndexingConnectionSiteException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    @ExceptionHandler(SearchEmptyTermException.class)
    public ResponseEntity<ErrorResponse> handleException(SearchEmptyTermException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SearchNoReadyIndexException.class)
    public ResponseEntity<ErrorResponse> handleException(SearchNoReadyIndexException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SearchQueryTooExtensiveException.class)
    public ResponseEntity<ErrorResponse> handleException(SearchQueryTooExtensiveException ex) {
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
