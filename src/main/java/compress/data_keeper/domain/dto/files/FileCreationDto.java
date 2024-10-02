package compress.data_keeper.domain.dto.files;

import compress.data_keeper.domain.dto.UserParameters;
import compress.data_keeper.domain.dto.folders.FolderDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Schema(description = "DTO with file for save in the bucket")
public class FileCreationDto extends UserParameters {

    @Schema(description = "File for save in the bucket")
    @NotNull(message = "File cannot be null")
    private MultipartFile file;

    @Schema(description = "Name of folder", example = "My invoices")
    @Size(min = 3, max = 20, message = "Folder name must be between 3 and 20 characters")
    @NotBlank(message = "Folder name must not be empty")
    private String folderName;

    @Schema(description = "Description of folder", example = "My invoices")
    @Size(max = 200, message = "Folder description must be no more than 200 characters.")
    private String folderDescription;

    @Schema(description = "Path of folder",  example = "http://bucket/isiisi/23")
    private String folderPath;
}
