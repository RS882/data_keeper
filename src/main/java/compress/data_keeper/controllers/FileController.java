package compress.data_keeper.controllers;

import compress.data_keeper.controllers.API.FileAPI;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.services.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
            Long userId,
            int page, int size, String sortBy, Boolean isAsc) {
        return ResponseEntity.ok(fileService.findFilesByUserId(
                userId,
                getPageable(page, size, sortBy, isAsc)));
    }
}
