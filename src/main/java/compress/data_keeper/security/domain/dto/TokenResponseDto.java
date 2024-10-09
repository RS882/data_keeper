package compress.data_keeper.security.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "JSON Access Web Token")
public class TokenResponseDto {
    @Schema(description = "User id", example = "236")
    private Long userId;

    @Schema(description = "Access token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0dXNlcjFAbWFpbC5jb20iLCJleHAiOjE3MjA3MDEyNzQsImlzcyI6IkF1dGhvcml6YXRpb24iLCJpYXQiOjE3MjA2OTk0NzQsInJvbGUiOlsiUk9MRV9VU0VSIl0sImVtYWlsIjoidGVzdHVzZXIxQG1haWwuY29tIn0.S6QwOKRtYcii5rSwrnUoKCvJAhHiSrZmi59Mhjn-yRI7xA3rEUPQw5gg-w")
    private String accessToken;

    @JsonCreator
    public TokenResponseDto(@JsonProperty("userId") Long userId,
                            @JsonProperty("accessToken") String accessToken) {
        this.userId = userId;
        this.accessToken = accessToken;
    }
}
