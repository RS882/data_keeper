package compress.data_keeper.domain.dto.videos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoDto {
    @NotNull(message = "File is missing")
    private MultipartFile file;
}
