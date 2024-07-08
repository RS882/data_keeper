package compress.video.exception_handler.bad_requeat.exceptions;

import compress.video.exception_handler.bad_requeat.BadRequestException;

public class BadFileSizeException extends BadRequestException {
    public BadFileSizeException(String fileName, long fileSize, long maxSize) {
        super(String.format("Size of file  <%s> : <%d> is greater than <%d>",
                fileName, fileSize, maxSize));
    }

    public BadFileSizeException() {
        super(String.format("File is empty"));
    }
}
