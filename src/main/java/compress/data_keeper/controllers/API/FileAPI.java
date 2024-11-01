package compress.data_keeper.controllers.API;

import compress.data_keeper.domain.dto.ResponseMessageDto;
import compress.data_keeper.domain.dto.files.*;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.dto.ValidationErrorsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "File Controller", description = "Controller for CRUD operation with file")
@RequestMapping("/v1/file")
public interface FileAPI {

    String PAGE_VALUE = "0";
    String SIZE_VALUE = "10";
    String SORT_BY = "createdAt";

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

            @ApiResponse(responseCode = "400", description = "Request is wrong",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    oneOf = {
                                            ValidationErrorsDto.class
                                    }
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Errors",
                                            value = "{\n" +
                                                    "  \"errors\": [\n" +
                                                    "    {\n" +
                                                    "      \"field\": \"FileCreationDto.file\",\n" +
                                                    "      \"message\": \"File cannot be null\",\n" +
                                                    "      \"rejectedValue\": \"rt\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            })),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Not found",
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


    @Operation(summary = "Save file to bucket when user is owner this file",
            description = "This method allows you to save uploaded file to a bucket.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File saved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDto.class))),

            @ApiResponse(responseCode = "400", description = "Request is wrong",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    oneOf = {
                                            ValidationErrorsDto.class,
                                            ResponseMessageDto.class
                                    }
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Errors",
                                            value = "{\n" +
                                                    "  \"errors\": [\n" +
                                                    "    {\n" +
                                                    "      \"field\": \"FileDto.filePath\",\n" +
                                                    "      \"message\": \"Path cannot be empty or blank\",\n" +
                                                    "      \"rejectedValue\": \"rt\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "File is empty",
                                            value = "{\"message\": \"File is empty\"}"
                                    )
                            })),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Not found",
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
            @org.springframework.web.bind.annotation.RequestBody
            @Parameter(description = "DTO with file information")
            FileDto dto,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser
    );

    @Operation(summary = "Get all links of file when user is admin",
            description = "This method allows you to get all links of file when you are admin."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Response get successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDtoWithPagination.class))),

            @ApiResponse(responseCode = "400",
                    description = "Invalid input",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
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
    @GetMapping("/all")
    ResponseEntity<FileResponseDtoWithPagination> getAllFilesLinks(
            @RequestParam(defaultValue = PAGE_VALUE)
            @Parameter(description = "Requested page number. ", example = "0")
            int page,
            @RequestParam(defaultValue = SIZE_VALUE)
            @Parameter(description = "Number of entities per page.", example = "10")
            int size,
            @RequestParam(defaultValue = SORT_BY)
            @Parameter(description = "Sorting field.", examples = {
                    @ExampleObject(name = "Sort by created time(default)", value = "createdAt"),
                    @ExampleObject(name = "Sort by update time", value = "updatedAt"),
                    @ExampleObject(name = "Sort by bucket name", value = "bucketName")
            })
            String sortBy,
            @RequestParam(defaultValue = "true")
            @Parameter(description = "Sorting direction.", examples = {
                    @ExampleObject(name = "Sort direction is ascending(default)", value = "true"),
                    @ExampleObject(name = "Sort direction is descending", value = "false")
            })
            Boolean isAsc
    );

    @Operation(summary = "Get all links of file by user id when user id equals current user id or user is admin",
            description = "This method allows you to get all links of file by user id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Response get successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDtoWithPagination.class))),

            @ApiResponse(responseCode = "400",
                    description = "Invalid input",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
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
    @GetMapping("/all/user/{id}")
    ResponseEntity<FileResponseDtoWithPagination> getFilesLinksByUserId(
            @Valid
            @PathVariable
            @Parameter(description = "User ID. Minimum value is 1", example = "11")
            @Min(1)
            Long id,
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            User currentUser,
            @RequestParam(defaultValue = PAGE_VALUE)
            @Parameter(description = "Requested page number. ", example = "0")
            int page,
            @RequestParam(defaultValue = SIZE_VALUE)
            @Parameter(description = "Number of entities per page.", example = "10")
            int size,
            @RequestParam(defaultValue = SORT_BY)
            @Parameter(description = "Sorting field.", examples = {
                    @ExampleObject(name = "Sort by created time(default)", value = "createdAt"),
                    @ExampleObject(name = "Sort by update time", value = "updatedAt"),
                    @ExampleObject(name = "Sort by bucket name", value = "bucketName")
            })
            String sortBy,
            @RequestParam(defaultValue = "true")
            @Parameter(description = "Sorting direction.", examples = {
                    @ExampleObject(name = "Sort direction is ascending(default)", value = "true"),
                    @ExampleObject(name = "Sort direction is descending", value = "false")
            })
            Boolean isAsc
    );

    @Operation(summary = "Get link of file by file id when file's owner equals current user or user is admin",
            description = "This method allows you to get link of file by file id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Response get successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDtoWithPagination.class))),

            @ApiResponse(responseCode = "400",
                    description = "Invalid input",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "File not found",
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
    @GetMapping("/{id}")
    ResponseEntity<FileResponseDto> getFileLinkByFileId(
            @Valid
            @PathVariable
            @Parameter(description = "File ID", example = "24fa7e1f-e9cc-4292-af16-72de8754d10b")
            @NotNull(message = "File Id can not be empty")
            UUID id,
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            User currentUser
    );

    @Operation(summary = "Update file information when user is owner this file or admin",
            description = "This method allows you to update file information.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File information updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileResponseDto.class))),

            @ApiResponse(responseCode = "400", description = "Request is wrong",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    oneOf = {
                                            ValidationErrorsDto.class,
                                            ResponseMessageDto.class
                                    }
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Errors",
                                            value = "{\n" +
                                                    "  \"errors\": [\n" +
                                                    "    {\n" +
                                                    "      \"field\": \"FileUpdateDto.fileId\",\n" +
                                                    "      \"message\": \"Id cannot be null\",\n" +
                                                    "      \"rejectedValue\": \"rt\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "Bad value",
                                            value = "{\"message\": \"Dto is null\"}"
                                    )
                            })),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Not found",
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
    @PutMapping("/update/info")
    ResponseEntity<FileResponseDto> updateFileInfo(
            @Valid
            @NotNull
            @org.springframework.web.bind.annotation.RequestBody
            FileUpdateDto fileUpdateDto,
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            User currentUser
    );

    @Operation(summary = "Delete file by file id when file owner equals current user or user is admin",
            description = "This method allows you to delete file by file id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",description = "File deleted successful"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid input",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "403",
                    description = "User doesn't have right for this resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseMessageDto.class)
                    )),
            @ApiResponse(responseCode = "404",
                    description = "File not found",
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
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteFileById(
            @Valid
            @PathVariable
            @Parameter(description = "File ID", example = "24fa7e1f-e9cc-4292-af16-72de8754d10b")
            @NotNull(message = "File Id can not be empty")
            UUID id,
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            User currentUser
    );
}
