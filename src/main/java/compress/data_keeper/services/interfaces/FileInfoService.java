package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.entity.FileInfo;

import java.util.List;
import java.util.UUID;

public interface FileInfoService {
    FileInfo createFileInfo(FileInfoDto dto);

    List<FileInfo> createFileInfo(List<FileInfoDto> dtos);

    List<FileInfo> getFileInfoByFolderId(UUID folderId);

    void deleteAllFileInfosByFolderId(UUID folderId);

    List<FileInfo> updateFileInfos(List<FileInfo> fileInfos);
}
