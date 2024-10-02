package compress.data_keeper.exception_handler.server_exception.exceptions;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;

public class MinioGetUrlException extends ServerIOException {
    public MinioGetUrlException(String message) {
        super(message);
    }
}
