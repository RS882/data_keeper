package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
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

    FileInfo createFileInfo(FileInfoDto dto);

    List<FileInfo> createFileInfo(List<FileInfoDto> dtos);

    FileInfo findOriginalFileInfoById(UUID id);

    List<FileInfo> getFilesInfosByFolderIdAndOriginalFileId(UUID folderId, UUID fileId);

    void deleteAllFileInfosByFolderId(UUID folderId);

    FileInfo updateFileInfo(UUID fileId, FileInfoDto dto);
}
