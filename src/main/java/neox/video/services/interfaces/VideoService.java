package neox.video.services.interfaces;

import neox.video.constants.VideoProperties;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {
    void compress(String inputFile, String outputFile, String originalFileName, VideoProperties quality);
    void save(MultipartFile file, VideoProperties quality);

}
