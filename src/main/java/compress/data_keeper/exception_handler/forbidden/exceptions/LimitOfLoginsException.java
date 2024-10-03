package compress.data_keeper.exception_handler.forbidden.exceptions;

import compress.data_keeper.exception_handler.forbidden.ForbiddenException;

public class LimitOfLoginsException extends ForbiddenException {

    public LimitOfLoginsException(Long userId) {
        super(String.format("User with ID %d has limit of logins.", userId));
    }
}
