package compress.data_keeper.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DTO with file response information after operation with file")
public class FileResponseDto {

    @Schema(description = "Link for downloading file from bucket", example = "http://bucket/isiisi/23/file.txt")
    String linkToFile;

    @Schema(description = "The length of time the link will be active in milliseconds", example = "1234599200")
    long linkIsValidForMs;
}
