package compress.data_keeper.domain.dto.folders;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderDto {

    private String name;

    private String description;

    private String path;

    private String bucketName;

    private boolean isFolderProtected;
}
