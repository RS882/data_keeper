package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.*;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


public interface FileService {

    String ORIGINAL_FILE_KEY = "originalFile";

    FileResponseDto uploadFileTemporary(FileCreationDto fileCreationDto, User user);

    FileResponseDto saveTemporaryFile(FileDto dto, User user);

    FileResponseDtoWithPagination findAllFiles(Pageable pageable);

    FileResponseDtoWithPagination findFilesByUserId(Long userId, User currentUser, Pageable pageable);

    FileResponseDto findFileByFileId(UUID fileId, User currentUser);

    FileResponseDto updateFileInfo(FileUpdateDto fileUpdateDto, User currentUser);

    FileInfo createFileInfo(FileInfoDto dto);

    List<FileInfo> createFileInfo(List<FileInfoDto> dtos);

    FileInfo findOriginalFileInfoById(UUID id);

    List<FileInfo> findFilesInfosByFolderIdAndOriginalFileId(UUID folderId, UUID fileId);

    void deleteAllFileInfosByFolderId(UUID folderId);

    void deleteFileById(UUID id, User currentUser);

    void deleteFilesInfos(List<FileInfo> fileInfos);

    List<FileInfo> findOldTempFiles(String bucketName, long secondsInterval);

    FileInfo updateFileInfo(UUID fileId, FileInfoDto dto);

}
