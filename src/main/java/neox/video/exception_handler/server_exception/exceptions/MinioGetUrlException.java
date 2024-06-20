package neox.video.exception_handler.server_exception.exceptions;

import neox.video.exception_handler.server_exception.ServerIOException;

public class MinioGetUrlException extends ServerIOException {
    public MinioGetUrlException(String message) {
        super(message);
    }
}
