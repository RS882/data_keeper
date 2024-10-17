package compress.data_keeper.exception_handler.forbidden.exceptions;

import compress.data_keeper.exception_handler.forbidden.ForbiddenException;

public class UserDoesntHaveRightException extends ForbiddenException {
    public UserDoesntHaveRightException(String email) {
        super(String.format("User with email '%s' does not have right to do this resource", email));
    }

}
