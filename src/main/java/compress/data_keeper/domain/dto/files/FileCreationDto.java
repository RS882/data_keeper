package compress.data_keeper.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@Schema(description = "DTO with file for save in the temporary bucket")
public class FileCreationDto {

    @Schema(description = "File for save in the temporary bucket")
    @NotNull(message = "File cannot be null")
    private MultipartFile file;

    @Schema(description = "Description of file", example = "My best photo")
    @Size(max = 200, message = "File description must be no more than 200 characters.")
    private String fileDescription;

    @Schema(description = "Name of folder", example = "My invoices")
    @Size(max = 20, message = "Folder name must be no more than 20 characters")
    private String folderName;

    @Schema(description = "Description of folder", example = "In this folder I saved my invoices from the dentist.")
    @Size(max = 200, message = "Folder description must be no more than 200 characters.")
    private String folderDescription;

    @Schema(description = "Path of folder", example = "http://bucket/isiisi/23")
    private String folderPath;

    @Schema(description = "Is folder protected from deleting or not", examples = {"false", "true"}, defaultValue = "false")
    @Builder.Default
    private Boolean isFolderProtected = false;
}
