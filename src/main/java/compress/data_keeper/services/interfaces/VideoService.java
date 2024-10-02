package compress.data_keeper.services.interfaces;


import compress.data_keeper.constants.VideoProperties;
import compress.data_keeper.domain.dto.videos.VideoResponseDto;
import org.springframework.web.multipart.MultipartFile;


public interface VideoService {
    VideoResponseDto save(MultipartFile file, VideoProperties quality,boolean isPublic);

}
