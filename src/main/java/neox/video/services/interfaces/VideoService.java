package neox.video.services.interfaces;

import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface VideoService {
    void compress(Path inputFile, Path outputFile, String originalFileName, VideoProperties quality);
    VideoResponseDto save(MultipartFile file, VideoProperties quality);

}
