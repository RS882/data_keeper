package compress.data_keeper.controllers;

import compress.data_keeper.controllers.API.FileAPI;
import compress.data_keeper.domain.dto.files.*;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.services.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static compress.data_keeper.services.utilities.PaginationUtilities.getPageable;

@RestController
@RequiredArgsConstructor
public class FileController implements FileAPI {

    private final FileService fileService;

    @Override
    public ResponseEntity<FileResponseDto> saveFileTemporarily(FileCreationDto fileCreationDto, User currentUser) {
        return ResponseEntity.ok(fileService.uploadFileTemporary(fileCreationDto, currentUser));
    }

    @Override
    public ResponseEntity<FileResponseDto> saveFileToBucket(FileDto dto, User currentUser) {
        return ResponseEntity.ok(fileService.saveTemporaryFile(dto, currentUser));
    }

    @Override
    public ResponseEntity<FileResponseDtoWithPagination> getAllFilesLinks(
            int page, int size, String sortBy, Boolean isAsc) {
        return ResponseEntity.ok(fileService.findAllFiles(
                getPageable(page, size, sortBy, isAsc)));
    }

    @Override
    public ResponseEntity<FileResponseDtoWithPagination> getFilesLinksByUserId(
            Long id, User currentUser,
            int page, int size, String sortBy, Boolean isAsc) {
        return ResponseEntity.ok(fileService.findFilesByUserId(
                id, currentUser,
                getPageable(page, size, sortBy, isAsc)));
    }

    @Override
    public ResponseEntity<FileResponseDto> getFileLinkByFileId(UUID id, User currentUser) {
        return ResponseEntity.ok(fileService.findFileByFileId(id, currentUser));
    }

    @Override
    public ResponseEntity<FileResponseDto> updateFileInfo(FileUpdateDto fileUpdateDto, User currentUser) {
        return ResponseEntity.ok(fileService.updateFileInfo(fileUpdateDto, currentUser));
    }

    @Override
    public void deleteFileById(UUID id, User currentUser) {
        fileService.deleteFileById(id, currentUser);
    }
}
