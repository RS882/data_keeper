package compress.video.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import compress.video.constants.VideoProperties;
import compress.video.domain.dto.VideoDto;
import compress.video.domain.dto.VideoResponseDto;
import compress.video.services.interfaces.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
