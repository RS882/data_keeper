package compress.data_keeper.domain.dto.files;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "DTO for updating file information")
public class FileUpdateDto {

    @Schema(description = "Id of file from bucket", example = "c7593273-6287-4a37-ad27-f8f07f9a36f1")
    @NotNull(message = "Id cannot be null")
    private UUID fileId;

    @Schema(description = "Original file name", example = "new file.txt")
    @Size(max = 30, message = "Folder name must be no more than 30 characters")
    private String fileName;

    @Schema(description = "Description of file", example = "My best photo")
    @Size(max = 200, message = "File description must be no more than 200 characters.")
    private String fileDescription;

    @Schema(description = "Name of folder", example = "My invoices")
    @Size(max = 20, message = "Folder must be no more than 20 characters")
    private String folderName;

    @Schema(description = "Description of folder", example = "In this folder I saved my invoices from the dentist.")
    @Size(max = 200, message = "Folder description must be no more than 200 characters.")
    private String folderDescription;

    @Schema(description = "Is folder protected from deleting or not", examples = {"false", "true"})
    @Builder.Default
    private Boolean isFolderProtected = false;
}
