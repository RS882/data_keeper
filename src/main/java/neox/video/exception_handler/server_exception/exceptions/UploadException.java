package neox.video.exception_handler.server_exception.exceptions;

import neox.video.exception_handler.server_exception.ServerException;

public class UploadException extends ServerException {
    public UploadException(String fileName) {
        super(String.format("The video file %s cannot be downloaded", fileName));
    }
}
