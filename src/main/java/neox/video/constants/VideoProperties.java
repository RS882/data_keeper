package neox.video.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import neox.video.domain.dto.VideoPropsDto;
import neox.video.exception_handler.not_found.exceptions.VideoPropertiesNotFoundException;

@RequiredArgsConstructor
@Getter
public enum VideoProperties {
    HD(VideoPropsDto.builder()
            .videoBitrate(4_000_000)
            .width(1280)
            .height(720)
            .audioBitrate(128_000)
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
//    HD(4_000_000, 1280, 720,
//            60, 128_000, 100_000),
//    FULL_HD(6_000_000, 1920, 1080,
//            60, 192_000, 200_000);

    //    private final int videoBitrate;
//    private final int width;
//    private final int height;
//    private final double frameRate;
//    private final int audioBitrate;
//    private final long maxSize;
    private final VideoPropsDto videoProps;

    public static VideoProperties get(String quality) {
        try {
            return VideoProperties.valueOf(quality.toUpperCase());
        } catch (Exception e) {
            throw new VideoPropertiesNotFoundException(quality);
        }
    }
}

