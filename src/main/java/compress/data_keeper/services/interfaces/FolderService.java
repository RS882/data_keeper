package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;

public interface FolderService {

    Folder getFolder(FolderDto dto, User user, String dirPrefix);

    Folder getFolderByFolderPath(String path);
}
