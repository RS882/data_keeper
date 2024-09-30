package compress.video.services;

import compress.video.domain.dto.files.FileResponseDto;
import compress.video.exception_handler.bad_requeat.exceptions.BadFileFormatException;
import compress.video.exception_handler.bad_requeat.exceptions.BadFileSizeException;
import compress.video.services.interfaces.DataStorageService;
import compress.video.services.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static compress.video.services.utilities.FileUtilities.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${storage.url}")
    private String storageUrl;

    @Value("${prefix.dir}")
    private String dirPrefix;

    @Value("${prefix.private}")
    private String prefixPrivate;

    @Value("${prefix.public}")
    private String prefixPublic;

    @Override
    public FileResponseDto uploadFile(MultipartFile file) {

        checkFile(file);

        UUID uuid = UUID.randomUUID();

        String fileExtension = getFileExtension(file);

        Path path = Path.of(dirPrefix, prefixPublic + uuid);

        Path outputFilePath = path.resolve(uuid + fileExtension);

        dataStorageService.uploadFIle(file, outputFilePath.toString());

        String filePath = toUnixStylePath(outputFilePath.toString());

        return FileResponseDto.builder()
                .fileUrl(filePath)
                .build();
    }
}
