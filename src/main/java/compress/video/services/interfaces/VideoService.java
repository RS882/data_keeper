package compress.video.services.interfaces;


import compress.video.constants.VideoProperties;
import compress.video.domain.dto.VideoResponseDto;
import org.springframework.web.multipart.MultipartFile;


public interface VideoService {
    VideoResponseDto save(MultipartFile file, VideoProperties quality,boolean isPublic);

}
