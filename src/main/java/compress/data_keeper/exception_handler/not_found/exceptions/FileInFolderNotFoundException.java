package compress.data_keeper.exception_handler.not_found.exceptions;

import compress.data_keeper.exception_handler.not_found.NotFoundException;

import java.util.UUID;

public class FileInFolderNotFoundException extends NotFoundException {
    public FileInFolderNotFoundException(UUID folderId) {
        super(String.format("Files not found in folder '%s'", folderId));
    }

}
