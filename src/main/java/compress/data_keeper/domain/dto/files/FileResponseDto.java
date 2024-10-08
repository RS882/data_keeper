package compress.data_keeper.domain.dto.files;

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
}
