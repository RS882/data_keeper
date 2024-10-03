package compress.data_keeper.domain.dto.users;

import compress.data_keeper.domain.dto.UserParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO with user information")
public class UserDto extends UserParameters {

    @Schema(description = "User name", example = "John")
    private String userName;

    @Schema(description = "User email", example = "example@gmail.com")
    private String email;
}
