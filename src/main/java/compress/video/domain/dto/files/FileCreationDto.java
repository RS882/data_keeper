package compress.video.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "DTO with file for save in the bucket")
public class FileCreationDto {

    @Schema(description = "File for save in the bucket")
    @NotNull(message = "File cannot be null")
    private MultipartFile file;
}
