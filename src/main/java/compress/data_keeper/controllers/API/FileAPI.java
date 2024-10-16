package compress.data_keeper.controllers.API;

import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.domain.dto.ResponseMessageDto;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "File Controller", description = "Controller for CRUD operation with file")
@RequestMapping("/v1/file")
public interface FileAPI {
    @Operation(summary = "Upload file to temp bucket",
            description = "This method allows you to upload a file to a temporary bucket.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = FileCreationDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDto.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
    })
    @PostMapping("/temp")
    ResponseEntity<FileResponseDto> saveFileTemporarily(
            @ModelAttribute
            @Valid
            FileCreationDto fileCreationDto,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser);


    @Operation(summary = "Save file to  bucket",
            description = "This method allows you to save uploaded file to a bucket.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File saved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDto.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "500",
                    description = "Server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
    })
    @PatchMapping("/save")
    ResponseEntity<FileResponseDto> saveFileToBucket(
            @Valid
            @RequestBody
            @Parameter(description = "DTO with file information")
            FileDto dto,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser
    );

//    @DeleteMapping("/temp")
//    void deleteTemporarilyFile();
//

}
