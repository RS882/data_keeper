package neox.video.exception_handler.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@Schema(name = "Validation Error", description = "Validation error description")
public class ValidationErrorDto {

//    @Schema(description = "the name of the field where the error occurred", example = "field name")
   private String field;

//    @Schema(description = "the value entered by the user and which was rejected by the server", example = "value of the field")
    private String rejectedValue;

//    @Schema(description = "the message to show for the user", example = "error message")
    private String message;
}