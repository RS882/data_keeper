package compress.data_keeper.controllers.API;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.security.domain.AuthInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name="File Controller", description = "Controller for CRUD operation with file")
@RequestMapping("/v1/file")
public interface FileAPI {
    @Operation(summary = "Upload file to bucket",
            description = "This method allows you to upload a file to a bucket.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(implementation = FileCreationDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FileResponseDto.class))),
    })
    @PostMapping
    ResponseEntity<FileResponseDto> uploadFile(
            @ModelAttribute
            @Valid
            FileCreationDto fileCreationDto,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser);
}
