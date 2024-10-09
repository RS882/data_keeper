package compress.data_keeper.domain.dto.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "DTO with file response information after operation with file")
public class FileResponseDto {

    public static final String ORIGINAL_FILE_KEY = "originalFile";

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

    @Schema(description = "Paths of file from bucket",
            example = """
                    {
                        "originalFile": http://bucket/isiisi/23/file.txt,
                        "320x320": http://bucket/isiisi/23/file.jpg
                    }
                    """)
    private Map<String, String> paths;

    @JsonCreator
    public FileResponseDto(
            @JsonProperty("linksToFiles") Map<String, String> linksToFiles,
            @JsonProperty("linksIsValidForMs") long linksIsValidForMs,
            @JsonProperty("paths") Map<String, String> paths) {
        this.linksToFiles = linksToFiles;
        this.linksIsValidForMs = linksIsValidForMs;
        this.paths = paths;
    }
}
