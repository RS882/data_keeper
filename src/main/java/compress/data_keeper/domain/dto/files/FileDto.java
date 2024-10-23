package compress.data_keeper.domain.dto.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "DTO for operation with file")
public class FileDto {

    @Schema(description = "Id of file from bucket", example = "c7593273-6287-4a37-ad27-f8f07f9a36f1")
    @NotNull(message = "Id cannot be null")
    private UUID fileId;

    @JsonCreator
    public FileDto(@JsonProperty("fileId") UUID fileId) {
        this.fileId = fileId;
    }
}
