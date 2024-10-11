package compress.data_keeper.services;

import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.FolderDtoMapperService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.domain.CustomMultipartFile.toCustomMultipartFile;
import static compress.data_keeper.domain.dto.files.FileResponseDto.ORIGINAL_FILE_KEY;
import static compress.data_keeper.services.utilities.FileActionUtilities.getFileActionServiceByContentType;
import static compress.data_keeper.services.utilities.FileUtilities.checkFile;
import static compress.data_keeper.services.utilities.FileUtilities.getNameFromSizes;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    private final FolderService folderService;

    private final FileInfoService fileInfoService;

    private final FolderDtoMapperService folderDtoMapperService;


    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Value("${prefix.dir}")
    private String dirPrefix;

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Override
    @Transactional
    public FileResponseDto uploadFileTemporary(FileCreationDto fileCreationDto, User user) {

        MultipartFile file = toCustomMultipartFile(fileCreationDto.getFile());

        checkFile(file);

        final Folder folderForFile = folderService.getFolder(folderDtoMapperService.toDto(fileCreationDto), user, tempFolder);

        FileInfoDto fileInfoDto = new FileInfoDto(file, folderForFile, fileCreationDto.getFileDescription());
        fileInfoDto.setIsOriginalFile(true);
        final FileInfo fileInfo = fileInfoService.createFileInfo(fileInfoDto);

        final String originalFilePath = dataStorageService.uploadFIle(file, fileInfo.getPath()).object();

        Map<String, String> paths = new HashMap<>();
        Map<String, String> links = new HashMap<>();

        paths.put(ORIGINAL_FILE_KEY, originalFilePath);
        links.put(ORIGINAL_FILE_KEY, dataStorageService.getTempFullPath(originalFilePath));

        Map<String, String> imgPaths = createImagesForFile(
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

    private Map<String, String> createImagesForFile(
            MultipartFile file,
            String folderPath,
            String fileUUID) {

        final String imgFileName = fileUUID + "." + IMAGE_FORMAT;

        final FileActionService fileActionService = getFileActionService(file);

        final Folder folder = folderService.getFolderByPath(folderPath);

        Map<String, String> paths = new HashMap<>();

        List<FileInfoDto> fileInfoDtoList = new ArrayList<>();

        if (fileActionService != null) {

            fileActionService.getFileImages(file).forEach((key, value) -> {
                Path filePath = Path.of(folderPath, key, imgFileName);
                InputStreamDto dto = new InputStreamDto(
                        value,
                        key + "." + IMAGE_FORMAT,
                        MediaType.IMAGE_JPEG_VALUE);
                String imgFilePath = dataStorageService.uploadFIle(dto, filePath.toString()).object();

                FileInfoDto fileInfoDto = new FileInfoDto(value, folder, imgFilePath, key);
                fileInfoDtoList.add(fileInfoDto);

                paths.put(key, imgFilePath);
            });

            fileInfoService.createFileInfo(fileInfoDtoList);
        } else {
            paths.putAll(getNullImagesPaths());
        }
        return paths;
    }

    private Map<String, String> getNullImagesPaths() {
        Map<String, String> paths = new HashMap<>();
        IMAGE_SIZES.forEach(size -> paths.put(getNameFromSizes(size), null));
        return paths;
    }

    private Map<String, String> getTempImagesLinks(Map<String, String> paths) {
        Map<String, String> links = new HashMap<>();
        paths.forEach((key, value) ->
                links.put(key, dataStorageService.getTempFullPath(value))
        );
        return links;
    }

    private FileActionService getFileActionService(MultipartFile file) {

        Tika tika = new Tika();
        try {
            String contentType = tika.detect(file.getInputStream());

            return getFileActionServiceByContentType(contentType);
        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
