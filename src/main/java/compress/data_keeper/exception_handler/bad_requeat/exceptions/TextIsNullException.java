package compress.data_keeper.exception_handler.bad_requeat.exceptions;

import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;

public class TextIsNullException extends BadRequestException {
    public TextIsNullException() {
        super("Text is null");
    }
}
