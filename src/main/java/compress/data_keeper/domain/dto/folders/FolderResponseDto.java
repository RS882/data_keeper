package compress.data_keeper.domain.dto.folders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "DTO with folder information ")
public class FolderResponseDto {

    @Schema(description = "Is the folder empty or not?", examples = {"true", "false"}, defaultValue = "false")
    private boolean isFolderEmpty;

    @Schema(description = "Folder ID", example = "c7593273-6287-4a37-ad27-f8f07f9a36f1")
    private UUID folderId;

    @Schema(description = "Name of folder", example = "My invoices")
    private String folderName;

    @Schema(description = "Description of folder", example = "My invoices")
    private String folderDescription;
}
