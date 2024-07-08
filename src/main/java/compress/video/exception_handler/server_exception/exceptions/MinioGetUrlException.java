package compress.video.exception_handler.server_exception.exceptions;

import compress.video.exception_handler.server_exception.ServerIOException;

public class MinioGetUrlException extends ServerIOException {
    public MinioGetUrlException(String message) {
        super(message);
    }
}
