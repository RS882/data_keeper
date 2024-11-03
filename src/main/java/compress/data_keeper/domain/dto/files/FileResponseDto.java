package compress.data_keeper.domain.dto.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
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

    @Schema(description = "Date and time until which the link is valid", example = "2025-04-13T14:30:00")
    private ZonedDateTime linksIsValidUntil;

    @Schema(description = "FIle ID",
            example = "c7593273-6287-4a37-ad27-f8f07f9a36f1")
    private UUID fileId;

    @Schema(description = "Original file name", example = "new file.txt")
    private String fileName;

    @Schema(description = "File description", example = "My new file")
    private String fileDescription;

    @Schema(description = "Folder name", example = "Files for study")
    private String folderName;

    @Schema(description = "Folder description", example = "My files for lessons")
    private String folderDescription;

    @JsonCreator
    public FileResponseDto(
            @JsonProperty("linksToFiles") Map<String, String> linksToFiles,
            @JsonProperty("linksIsValidUntil") ZonedDateTime linksIsValidUntil,
            @JsonProperty("fileId") UUID fileId,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("fileDescription") String fileDescription,
            @JsonProperty("folderName") String folderName,
            @JsonProperty("folderDescription") String folderDescription) {
        this.linksToFiles = linksToFiles;
        this.linksIsValidUntil = linksIsValidUntil;
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileDescription = fileDescription;
        this.folderName = folderName;
        this.folderDescription = folderDescription;
    }
}
