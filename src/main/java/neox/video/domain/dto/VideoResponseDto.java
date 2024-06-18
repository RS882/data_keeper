package neox.video.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import neox.video.constants.VideoProperties;

import java.util.Map;

@Data
@AllArgsConstructor
public class VideoResponseDto {

    private Map<VideoProperties, Map<String, String>> values;
}
