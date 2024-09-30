package compress.video.controllers;

import compress.video.controllers.API.FileAPI;
import compress.video.domain.dto.files.FileCreationDto;
import compress.video.domain.dto.files.FileResponseDto;
import compress.video.services.interfaces.FileService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FileController implements FileAPI {

    private final FileService fileService;

    @Override
    public ResponseEntity<FileResponseDto> uploadFile(FileCreationDto fileCreationDto) {

        return ResponseEntity.ok(fileService.uploadFile(fileCreationDto.getFile()));
    }
}
