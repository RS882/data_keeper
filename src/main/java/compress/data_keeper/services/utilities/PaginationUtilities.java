package compress.data_keeper.services.utilities;

import compress.data_keeper.exception_handler.bad_requeat.exceptions.PaginationParameterIsWrongException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtilities {

    public static Pageable getPageable(int page, int size, String sortBy, Boolean isAsc) {
        try {
            Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(direction, sortBy));
        } catch (IllegalArgumentException e) {
            throw new PaginationParameterIsWrongException(page, size, sortBy);
        }
    }
}
