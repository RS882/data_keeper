package compress.video.services.interfaces;

import compress.video.domain.dto.files.FileResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileResponseDto uploadFile(MultipartFile file);
}
