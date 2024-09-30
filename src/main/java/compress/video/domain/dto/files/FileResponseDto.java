package compress.video.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO with file response information after operation with file")
public class FileResponseDto {

 @Schema(description = "Url for access to file in bucket", example = "http://bucket/isiisi/23/file.txt")
    String fileUrl;
}
