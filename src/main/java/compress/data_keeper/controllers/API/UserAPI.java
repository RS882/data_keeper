package compress.data_keeper.controllers.API;

import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User Controller", description = "Controller for CRUD operation with user")
@RequestMapping("/v1/user")
public interface UserAPI {

    @Operation(summary = "Create new user",
            description = "This method create new user from userDto.",
            requestBody = @RequestBody(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRegistrationDto.class)))
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDto.class))),
    })
    @PostMapping("/registration")
    ResponseEntity<UserDto> createUser(@org.springframework.web.bind.annotation.RequestBody @Valid UserRegistrationDto userRegistrationDto);
}
