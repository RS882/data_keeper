package compress.data_keeper.exception_handler.bad_requeat.exceptions;

import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;

public class BadFileBucketName extends BadRequestException {
    public BadFileBucketName(String message) {
        super(message);
    }
}
