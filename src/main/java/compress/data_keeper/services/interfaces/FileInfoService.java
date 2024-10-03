package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.FileInfo;
import compress.data_keeper.domain.Folder;
import org.springframework.web.multipart.MultipartFile;

public interface FileInfoService {
    FileInfo createFileInfo(MultipartFile file, Folder folder, String fileDescription);
}
