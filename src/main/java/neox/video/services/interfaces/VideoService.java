package neox.video.services.interfaces;


import neox.video.constants.VideoProperties;
import neox.video.domain.dto.VideoResponseDto;
import org.springframework.web.multipart.MultipartFile;


public interface VideoService {
    VideoResponseDto save(MultipartFile file, VideoProperties quality,boolean isPublic);

}
