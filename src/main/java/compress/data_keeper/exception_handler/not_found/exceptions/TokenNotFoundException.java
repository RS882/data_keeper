package compress.data_keeper.exception_handler.not_found.exceptions;


import compress.data_keeper.exception_handler.not_found.NotFoundException;

public class TokenNotFoundException extends NotFoundException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
