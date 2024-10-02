package compress.data_keeper.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response DTO with some message")
public class ResponseMessageDto {

    @Schema(description = "Message ", example = "Some text message")
    private String message;
}
