package compress.data_keeper.domain.dto;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Getter
@AllArgsConstructor
public  class InputStreamDto {

    InputStream inputStream;

    String originalFilename;

    String contentType;

    public static InputStreamDto getInputStreamDto(MultipartFile file, String contentType) {
        try {
            return getInputStreamDto(file.getInputStream(), file.getOriginalFilename(), contentType);
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    public static InputStreamDto getInputStreamDto(InputStream inputStream, String originalFilename, String contentType) {
        return new InputStreamDto(inputStream, originalFilename, contentType);
    }
}
