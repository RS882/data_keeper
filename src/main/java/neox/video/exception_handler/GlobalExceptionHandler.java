package neox.video.exception_handler;

import lombok.extern.slf4j.Slf4j;
import neox.video.domain.dto.ResponseMessageDto;
import neox.video.exception_handler.not_found.NotFoundException;
import neox.video.exception_handler.server_exception.ServerException;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ResponseMessageDto> handleException(ServerException e) {
        return  new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseMessageDto> handleException(BadRequestException e) {
        return  new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseMessageDto> handleException(NotFoundException e) {
        return  new ResponseEntity<>(new ResponseMessageDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

}
