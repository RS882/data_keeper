package compress.data_keeper.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import compress.data_keeper.constants.VideoProperties;
import compress.data_keeper.domain.dto.videos.VideoDto;
import compress.data_keeper.domain.dto.videos.VideoResponseDto;
import compress.data_keeper.services.interfaces.VideoService;
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
