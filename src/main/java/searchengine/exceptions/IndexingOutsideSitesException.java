package searchengine.exceptions;

public class IndexingOutsideSitesException extends RuntimeException {

    public IndexingOutsideSitesException() {
        super("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
    }
}
