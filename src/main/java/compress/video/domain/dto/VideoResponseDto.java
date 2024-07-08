package compress.video.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import compress.video.constants.VideoProperties;

import java.util.Map;

@Data
@AllArgsConstructor
public class VideoResponseDto {

    private Map<VideoProperties, Map<String, String>> values;
}
