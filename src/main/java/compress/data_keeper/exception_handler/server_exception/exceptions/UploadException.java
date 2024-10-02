package compress.data_keeper.exception_handler.server_exception.exceptions;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;

public class UploadException extends ServerIOException {
    public UploadException(String message) {
        super(message);
    }
}
