package compress.data_keeper.services;

import compress.data_keeper.domain.FileInfo;
import compress.data_keeper.domain.Folder;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import io.minio.ObjectWriteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.services.utilities.FileUtilities.checkFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    private final FolderService folderService;

    private final FileInfoService fileInfoService;

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Override
    @Transactional
    public FileResponseDto uploadFile(FileCreationDto fileCreationDto, User user) {

        MultipartFile file = fileCreationDto.getFile();

        checkFile(file);

        Folder folderForFile = folderService.getFolder(FolderDto.from(fileCreationDto), user);

        FileInfo fileInfo = fileInfoService.createFileInfo(file, folderForFile, fileCreationDto.getFileDescription());

        ObjectWriteResponse objectWriteResponse = dataStorageService.uploadFIle(file, fileInfo.getPath());

        String tempFilePath = dataStorageService.getTempFullPath(objectWriteResponse.object());

        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);

        return FileResponseDto.builder()
                .linkToFile(tempFilePath)
                .linkIsValidForMs(linkLifeTimeDuration)
                .build();
    }


}
