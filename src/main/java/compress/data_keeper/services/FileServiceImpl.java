package compress.data_keeper.services;

import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.services.utilities.FileUtilities.*;

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

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Override
    public FileResponseDto uploadFile(MultipartFile file) {

        checkFile(file);

        UUID uuid = UUID.randomUUID();

        String fileExtension = getFileExtension(file);

        Path path = Path.of(dirPrefix,  uuid.toString());

        Path outputFilePath = path.resolve(uuid + fileExtension);

        ObjectWriteResponse objectWriteResponse = dataStorageService.uploadFIle(file, outputFilePath.toString());

        String tempFilePath = dataStorageService.getTempFullPath(objectWriteResponse.object());

        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);

        return FileResponseDto.builder()
                .linkToFile(tempFilePath)
                .linkIsValidForMs(linkLifeTimeDuration)
                .build();
    }
}
