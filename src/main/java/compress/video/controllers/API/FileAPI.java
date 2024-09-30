package compress.video.controllers.API;

import compress.video.domain.dto.files.FileCreationDto;
import compress.video.domain.dto.files.FileResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name="File Controller", description = "Controller for CRUD operation with file")
@RequestMapping("/v1/file")
@Validated
public interface FileAPI {

//    @Operation(summary = "Save file in bucket")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful operation",
//                    requestBody = @RequestBody(
//                            content = @Content(mediaType = "multipart/form-data",
//                                    schema = @Schema(implementation = FileCreationDto.class)))
//    })
    @Operation(summary = "Upload file to bucket", description = "This method allows you to upload a file to a bucket.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(implementation = FileCreationDto.class))))
    @PostMapping
    ResponseEntity<FileResponseDto> uploadFile(@ModelAttribute FileCreationDto fileCreationDto);
}
