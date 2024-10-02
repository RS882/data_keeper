package compress.data_keeper.domain.dto.users;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.UserParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String userName;

    public static UserDto from(User user) {
        return UserDto.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .build();
    }
}
