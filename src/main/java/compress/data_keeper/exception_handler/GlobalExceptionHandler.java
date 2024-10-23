package compress.data_keeper.exception_handler;

import compress.data_keeper.exception_handler.forbidden.ForbiddenException;
import compress.data_keeper.exception_handler.unauthorized.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import compress.data_keeper.domain.dto.ResponseMessageDto;
import compress.data_keeper.exception_handler.dto.ValidationErrorDto;
import compress.data_keeper.exception_handler.dto.ValidationErrorsDto;
import compress.data_keeper.exception_handler.not_found.NotFoundException;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseMessageDto> handleNotFoundException(AuthenticationException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseMessageDto> handleNotFoundException(ForbiddenException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseMessageDto> handleException(HttpMessageNotReadableException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ResponseMessageDto> handleException(MissingRequestCookieException e) {
        return new ResponseEntity<>(new ResponseMessageDto("Cookie is incorrect"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseMessageDto> handleException(BadRequestException e) {
            return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseMessageDto> handleException(NotFoundException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseMessageDto> handleException(UnauthorizedException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ServerIOException.class)
    public ResponseEntity<ResponseMessageDto> handleException(ServerIOException e) {
        return new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseMessageDto> handleException(RuntimeException e) {
        log.error("RuntimeException occurred", e);
        return new ResponseEntity<>(new ResponseMessageDto("Something went wrong"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
