package compress.data_keeper.exception_handler.not_found.exceptions;

import compress.data_keeper.exception_handler.not_found.NotFoundException;

import java.util.UUID;

public class FileInfoNotFound extends NotFoundException {
    public FileInfoNotFound(UUID fileId) {
        super(String.format("File with ID <%s> not found", fileId));
    }
}
