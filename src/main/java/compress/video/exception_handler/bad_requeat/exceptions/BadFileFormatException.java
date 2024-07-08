package compress.video.exception_handler.bad_requeat.exceptions;

import compress.video.exception_handler.bad_requeat.BadRequestException;

public class BadFileFormatException extends BadRequestException {
    public BadFileFormatException(String originalFileName) {
        super(String.format("Bad file format: %s", originalFileName));
    }
}
