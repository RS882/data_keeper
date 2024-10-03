package compress.data_keeper.exception_handler.not_found.exceptions;

import compress.data_keeper.exception_handler.not_found.NotFoundException;

public class UserNotFoundByEmailException extends NotFoundException {
    public UserNotFoundByEmailException(String email) {
        super("User with email " + email + " not found");
    }
}
