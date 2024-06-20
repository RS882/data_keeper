package neox.video.exception_handler;

import lombok.extern.slf4j.Slf4j;
import neox.video.domain.dto.ResponseMessageDto;
import neox.video.exception_handler.dto.ValidationErrorDto;
import neox.video.exception_handler.dto.ValidationErrorsDto;
import neox.video.exception_handler.not_found.NotFoundException;
import neox.video.exception_handler.server_exception.ServerIOException;
import neox.video.exception_handler.bad_requeat.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ServerIOException.class)
    public ResponseEntity<ResponseMessageDto> handleException(ServerIOException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseMessageDto> handleException(BadRequestException e) {
            return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseMessageDto> handleException(NotFoundException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorsDto> handleValidationException(MethodArgumentNotValidException e) {
        List<ValidationErrorDto> validationErrors = new ArrayList<>();
        List<ObjectError> errors = e.getBindingResult().getAllErrors();

        for (ObjectError error : errors) {
            FieldError fieldError = (FieldError) error;

            ValidationErrorDto errorDto = ValidationErrorDto.builder()
                    .field(fieldError.getField())
                    .message("Field " + fieldError.getDefaultMessage())
                    .build();
            if (fieldError.getRejectedValue() != null)
                errorDto.setRejectedValue(fieldError.getRejectedValue().toString());

            validationErrors.add(errorDto);
        }
        return ResponseEntity.badRequest()
                .body(ValidationErrorsDto.builder()
                        .errors(validationErrors)
                        .build());
    }

}
