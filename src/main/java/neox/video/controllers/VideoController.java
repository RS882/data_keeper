package neox.video.controllers;

import io.minio.errors.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoDto;
import neox.video.domain.dto.VideoResponseDto;
import neox.video.services.interfaces.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


@RestController
@RequiredArgsConstructor
@RequestMapping("v1/video")
public class VideoController {

    private final VideoService service;

    @PostMapping
    public ResponseEntity<VideoResponseDto> saveNewVideo(
            @Valid
            VideoDto dto,
            @RequestParam(defaultValue = "HD")
            String quality,
            @RequestParam(defaultValue = "true")
            boolean isPublic) {
        return ResponseEntity.ok().body(
                service.save(
                        dto.getFile(),
                        VideoProperties.get(quality),
                        isPublic));
    }
}
