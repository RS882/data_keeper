package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.files.FileResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileResponseDto uploadFile(MultipartFile file);
}
