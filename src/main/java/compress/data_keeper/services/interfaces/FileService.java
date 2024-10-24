package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import org.springframework.data.domain.Pageable;


public interface FileService {

    String ORIGINAL_FILE_KEY = "originalFile";

    FileResponseDto uploadFileTemporary(FileCreationDto fileCreationDto, User user);

    FileResponseDto saveTemporaryFile(FileDto dto, User user);

    FileResponseDtoWithPagination findAllFiles(Pageable pageable);
}
