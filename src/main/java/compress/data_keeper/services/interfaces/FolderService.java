package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.Folder;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.folders.FolderDto;

public interface FolderService {

    Folder getFolder(FolderDto dto, User user, String dirPrefix);
}
