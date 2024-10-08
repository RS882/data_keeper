package compress.data_keeper.services;

import compress.data_keeper.domain.FileInfo;
import compress.data_keeper.domain.Folder;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.FolderDtoMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.domain.dto.files.FileResponseDto.ORIGINAL_FILE_KEY;
import static compress.data_keeper.services.utilities.FileUtilities.checkFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    private final FolderService folderService;

    private final FileInfoService fileInfoService;

    private final FolderDtoMapperService folderDtoMapperService;

    private final FileActionService fileActionService;

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Value("${prefix.dir}")
    private String dirPrefix;

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Override
    @Transactional
    public FileResponseDto uploadFileTemporary(FileCreationDto fileCreationDto, User user) {

        MultipartFile file = fileCreationDto.getFile();

        checkFile(file);

        Folder folderForFile = folderService.getFolder(folderDtoMapperService.toDto(fileCreationDto), user, tempFolder);

        FileInfo fileInfo = fileInfoService.createFileInfo(file, folderForFile, fileCreationDto.getFileDescription());

        String originalFilePath = dataStorageService.uploadFIle(file, fileInfo.getPath()).object();

        Map<String, String> paths = new HashMap<>();
        Map<String, String> links = new HashMap<>();

        paths.put(ORIGINAL_FILE_KEY, originalFilePath);
        links.put(ORIGINAL_FILE_KEY, dataStorageService.getTempFullPath(originalFilePath));

        Map<String, String> imgPaths = getImagesPaths(
                file,
                folderForFile.getPath(),
                fileInfo.getId().toString());

        paths.putAll(imgPaths);

        links.putAll(getTempImagesLinks(imgPaths));

        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);

        return FileResponseDto.builder()
                .linksToFiles(links)
                .linksIsValidForMs(linkLifeTimeDuration)
                .paths(paths)
                .build();
    }

    private Map<String, String> getImagesPaths(
            MultipartFile file,
            String folderPath,
            String fileUUID) {

        Map<String, String> paths = new HashMap<>();

        String imgFileName = fileUUID + "." + IMAGE_FORMAT;

        fileActionService.getFileImages(file).forEach((key, value) -> {
            Path filePath = Path.of(folderPath, key, imgFileName);

            InputStreamDto dto = new InputStreamDto(
                    value,
                    key + "." + IMAGE_FORMAT,
                    MediaType.IMAGE_JPEG_VALUE);

            String imgFilePath = dataStorageService.uploadFIle(dto, filePath.toString()).object();

            paths.put(key, imgFilePath);
        });
        return paths;
    }

    private Map<String, String> getTempImagesLinks(Map<String, String> paths) {

        Map<String, String> links = new HashMap<>();

        paths.forEach((key, value) ->
                links.put(key, dataStorageService.getTempFullPath(value))
        );
        return links;
    }
}
