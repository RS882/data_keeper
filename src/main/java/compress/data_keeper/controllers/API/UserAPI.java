package compress.data_keeper.controllers.API;

import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.users.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name="User Controller", description = "Controller for CRUD operation with user")
@RequestMapping("/v1/user")
public interface UserAPI {

    @Operation(summary = "Create new user",
            description = "This method create new user from userDto.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
    })
    @PostMapping
    ResponseEntity<UserDto> createUser(@org.springframework.web.bind.annotation.RequestBody @Valid UserDto userDto);
}
