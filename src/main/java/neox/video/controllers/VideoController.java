package neox.video.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoDto;
import neox.video.domain.dto.VideoResponseDto;
import neox.video.services.interfaces.VideoService;
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
            String quality
    ) {
        return ResponseEntity.ok().body(
                service.save(
                        dto.getFile(),
                        VideoProperties.get(quality)));
    }
}
