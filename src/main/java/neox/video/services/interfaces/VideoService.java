package neox.video.services.interfaces;

import io.minio.errors.*;
import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface VideoService {
//    void compress(Path inputFile, Path outputFile, String originalFileName, VideoProperties quality);
    VideoResponseDto save(MultipartFile file, VideoProperties quality) ;

}
