package compress.data_keeper.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.services.utilities.FileUtilities.checkFile;
import static compress.data_keeper.services.utilities.FileUtilities.getFileExtension;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    private final FolderService folderService;

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
    public FileResponseDto uploadFile(FileCreationDto fileCreationDto, User user) {

        MultipartFile file = fileCreationDto.getFile();

        checkFile(file);

        String folderName = folderService.createFolder(FolderDto.from(fileCreationDto), user);

        UUID uuid = UUID.randomUUID();

        String fileExtension = getFileExtension(file);

        Path outputFilePath = Path.of(folderName, uuid + fileExtension);


        ObjectWriteResponse objectWriteResponse = dataStorageService.uploadFIle(file, outputFilePath.toString());

        String tempFilePath = dataStorageService.getTempFullPath(objectWriteResponse.object());

        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);

        return FileResponseDto.builder()
                .linkToFile(tempFilePath)
                .linkIsValidForMs(linkLifeTimeDuration)
                .build();
    }
}
