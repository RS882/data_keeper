package compress.data_keeper.domain.dto.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@Schema(description = "DTO with file response information after operation with file")
public class FileResponseDto {

    @Schema(description = "Links for downloading files from bucket",
            example = """
                    {
                        "originalFile": http://bucket/isiisi/23/file.txt,
                        "320x320": http://bucket/isiisi/23/file.jpg
                    }
                    """)
    private Map<String, String> linksToFiles;

    @Schema(description = "The length of time the link will be active in milliseconds", example = "1234599200")
    private long linksIsValidForMs;

    @Schema(description = "FIle ID",
            example = "c7593273-6287-4a37-ad27-f8f07f9a36f1")
    private UUID fileId;

    @JsonCreator
    public FileResponseDto(
            @JsonProperty("linksToFiles") Map<String, String> linksToFiles,
            @JsonProperty("linksIsValidForMs") long linksIsValidForMs,
            @JsonProperty("fileId") UUID fileId) {
        this.linksToFiles = linksToFiles;
        this.linksIsValidForMs = linksIsValidForMs;
        this.fileId = fileId;
    }
}
