package compress.data_keeper.exception_handler.bad_requeat.exceptions;

import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;

public class UserIdIsNullException extends BadRequestException {
    public UserIdIsNullException() {
        super("User id is null!");
    }
}
