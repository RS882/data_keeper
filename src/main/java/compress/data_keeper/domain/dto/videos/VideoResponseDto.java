package compress.data_keeper.domain.dto.videos;

import lombok.AllArgsConstructor;
import lombok.Data;
import compress.data_keeper.constants.VideoProperties;

import java.util.Map;

@Data
@AllArgsConstructor
public class VideoResponseDto {

    private Map<VideoProperties, Map<String, String>> values;
}
