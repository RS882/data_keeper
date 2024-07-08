package compress.video.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import compress.video.domain.dto.VideoPropsDto;
import compress.video.exception_handler.not_found.exceptions.VideoPropertiesNotFoundException;

@RequiredArgsConstructor
@Getter
public enum VideoProperties {
    HD(VideoPropsDto.builder()
            .videoBitrate(4_000_000)
            .width(1280)
            .height(720)
            //TODO
            .audioBitrate(130_000)
            .frameRate(60)
            .maxSize(100_000 * 1024)
            .build()),
    FULL_HD(VideoPropsDto.builder()
            .videoBitrate(6_000_000)
            .width(1920)
            .height(1080)
            .audioBitrate(192_000)
            .frameRate(60)
            .maxSize(200_000 * 1024)
            .build());

    private final VideoPropsDto videoProps;

    public static VideoProperties get(String quality) {
        try {
            return VideoProperties.valueOf(quality.toUpperCase());
        } catch (Exception e) {
            throw new VideoPropertiesNotFoundException(quality);
        }
    }
}

