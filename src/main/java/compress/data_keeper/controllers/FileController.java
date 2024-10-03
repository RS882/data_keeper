package compress.data_keeper.controllers;

import compress.data_keeper.controllers.API.FileAPI;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FileController implements FileAPI {

    private final FileService fileService;

    private final UserService userService;

    @Override
    public ResponseEntity<FileResponseDto> uploadFile(FileCreationDto fileCreationDto, User currentUser) {
        return ResponseEntity.ok(fileService.uploadFile(fileCreationDto, currentUser));
    }
}
