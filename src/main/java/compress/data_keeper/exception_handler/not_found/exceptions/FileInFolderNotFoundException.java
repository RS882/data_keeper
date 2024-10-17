package compress.data_keeper.exception_handler.not_found.exceptions;

import compress.data_keeper.exception_handler.not_found.NotFoundException;

public class FileInFolderNotFoundException extends NotFoundException {
    public FileInFolderNotFoundException(String folderPath) {
        super(String.format("Files not found in folder '%s'", folderPath));
    }

}
