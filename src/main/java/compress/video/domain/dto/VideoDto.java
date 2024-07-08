package compress.video.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoDto {
    @NotNull(message = "File is missing")
    private MultipartFile file;
}
