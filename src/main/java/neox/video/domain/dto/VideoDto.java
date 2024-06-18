package neox.video.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoDto {
    private MultipartFile file;
}
