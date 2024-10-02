package compress.data_keeper.exception_handler.not_found.exceptions;

import compress.data_keeper.exception_handler.not_found.NotFoundException;

public class UserNotFoundByIdException extends NotFoundException {
    public UserNotFoundByIdException(long id) {
        super("User with id " + id + " not found");
    }
}
