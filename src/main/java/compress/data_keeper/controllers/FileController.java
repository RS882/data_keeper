package compress.data_keeper.controllers;

import compress.data_keeper.controllers.API.FileAPI;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.services.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
        return null;
    }
}
