package compress.data_keeper.exception_handler.bad_requeat.exceptions;

import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;

public class PaginationParameterIsWrongException extends BadRequestException {

    public PaginationParameterIsWrongException(int page, int size, String sortBy) {

        super(String.format("Parameters of pagination is wrong :" +
                " page <%d>, size <%d>, sortBy <%s>", page, size, sortBy));
    }
}

