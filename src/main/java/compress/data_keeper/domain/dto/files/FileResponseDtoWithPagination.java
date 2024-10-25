package compress.data_keeper.domain.dto.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
@Schema(description = "DTO with file response information after operation with file with pagination")
public class FileResponseDtoWithPagination {

    @Schema(description = "Set of files response DTO")
    private Set<FileResponseDto> files;

    @Schema(description = "Current page number", example = "6")
    private int pageNumber;

    @Schema(description = "Current page size", example = "20")
    private int pageSize;

    @Schema(description = "Total number of pages", example = "134")
    public int totalPages;

    @Schema(description = "Total number of elements", example = "345")
    private long totalElements;

    @Schema(description = "Is first page?", example = "true")
    private Boolean isFirstPage;

    @Schema(description = "Is last page?", example = "true")
    private Boolean isLastPage;

    @JsonCreator
    public FileResponseDtoWithPagination(
            @JsonProperty("files") Set<FileResponseDto> files,
            @JsonProperty("pageNumber") int pageNumber,
            @JsonProperty("pageSize") int pageSize,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("isFirstPage") Boolean isFirstPage,
            @JsonProperty("isLastPage") Boolean isLastPage) {
        this.files = files;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.isFirstPage = isFirstPage;
        this.isLastPage = isLastPage;
    }
}
