package compress.data_keeper.domain.dto.folders;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import javax.xml.transform.sax.SAXResult;

@Data
@Builder
@Schema(description = "DTO with folder information")
public class FolderDto {

    @Schema(description = "Name of folder", example = "My invoices")
    @Size(min = 3, max = 20, message = "Folder name must be between 3 and 20 characters")
    @NotBlank(message = "Folder name must not be empty")
    private String name;

    @Schema(description = "Description of folder", example = "My invoices")
    @Size(max = 200, message = "Folder description must be no more than 200 characters.")
    private String description;

    @Schema(description = "Path of folder", example = "http://bucket/isiisi/23")
    private String path;

    private String bucketName;
}
