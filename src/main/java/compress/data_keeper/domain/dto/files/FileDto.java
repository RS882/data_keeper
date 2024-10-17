package compress.data_keeper.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO for operation with file")
public class FileDto {

    @Schema(description = "Path of file from bucket", example = "http://bucket/isiisi/23/file.txt")
    @NotNull(message = "Path cannot be null")
    @NotBlank(message = "Path cannot be empty or blank")
    private String filePath;
}
