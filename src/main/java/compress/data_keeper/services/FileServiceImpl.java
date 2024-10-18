package compress.data_keeper.services;

import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.forbidden.exceptions.UserDoesntHaveRightException;
import compress.data_keeper.exception_handler.not_found.exceptions.FileInFolderNotFoundException;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.security.contstants.Role;
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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.domain.CustomMultipartFile.toCustomMultipartFile;
import static compress.data_keeper.services.utilities.FileActionUtilities.getFileActionServiceByContentType;
import static compress.data_keeper.services.utilities.FileUtilities.*;

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

        dataStorageService.uploadFIle(file, fileInfo.getPath()).object();

        List<FileInfo> filesInfos = new ArrayList<>();
        filesInfos.add(fileInfo);

        List<FileInfo> imgFilesInfos = createImagesForFile(
                file,
                folderForFile.getPath(),
                fileInfo.getId().toString());

        filesInfos.addAll(imgFilesInfos);

        return getFileResponseDtoByFileInfos(filesInfos);
    }

    private FileResponseDto getFileResponseDtoByFileInfos(List<FileInfo> fileInfos) {
        Map<String, String> paths = new HashMap<>();
        fileInfos.forEach(fi -> {
            String key = fi.getIsOriginalFile() ? ORIGINAL_FILE_KEY : fi.getDescription();
            String value = fi.getPath();
            paths.put(key, value);
        });
        Map<String, String> links = getTempFileLinks(paths);
        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);
        return FileResponseDto.builder()
                .linksToFiles(links)
                .linksIsValidForMs(linkLifeTimeDuration)
                .paths(paths)
                .build();
    }

    private List<FileInfo> createImagesForFile(
            MultipartFile file,
            String folderPath,
            String fileUUID) {

        final String imgFileName = fileUUID + "." + IMAGE_FORMAT;

        final FileActionService fileActionService = getFileActionService(file);

        final Folder folder = folderService.getFolderByPath(folderPath);

        List<FileInfoDto> fileInfoDtoList = new ArrayList<>();

        if (fileActionService != null) {
            Map<String, InputStream> imgFileStreams = fileActionService.getFileImages(file);
            imgFileStreams.forEach((key, value) -> {
                Path filePath = Path.of(folderPath, key, imgFileName);
                InputStreamDto dto = new InputStreamDto(
                        value,
                        key + "." + IMAGE_FORMAT,
                        MediaType.IMAGE_JPEG_VALUE);
                String imgFilePath = dataStorageService.uploadFIle(dto, filePath.toString()).object();

                FileInfoDto fileInfoDto = new FileInfoDto(value, folder, imgFilePath, key);
                fileInfoDtoList.add(fileInfoDto);
            });
            return fileInfoService.createFileInfo(fileInfoDtoList);
        } else {
            return getEmptyFilesInfos();
        }
    }

    private List<FileInfo> getEmptyFilesInfos() {

        return IMAGE_SIZES.stream().map(s ->
                FileInfo.builder()
                        .isOriginalFile(false)
                        .path(null)
                        .description(getNameFromSizes(s))
                        .build()
        ).collect(Collectors.toList());
    }

    private Map<String, String> getTempFileLinks(Map<String, String> paths) {
        Map<String, String> links = new HashMap<>();
        paths.forEach((key, value) ->
                links.put(key, dataStorageService.getTempLink(value))
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

    @Override
    @Transactional
    public FileResponseDto saveTemporaryFile(FileDto dto, User user) {

        String tempFilePath = dto.getFilePath();
        String folderPath = getFolderPathByFilePath(tempFilePath);
        Folder folder = folderService.getFolderByPath(folderPath);

        checkUserRights(folder, user);

        List<FileInfo> fileInfos = fileInfoService.getFileInfoByFolderId(folder.getId());
        if (fileInfos.isEmpty()) {
            throw new FileInFolderNotFoundException(folderPath);
        }
        List<FileInfo> updatedFileInfo = remoteFilesInBucket(fileInfos);

        String newFolderPath = getNewPath(folder.getPath());
        dataStorageService.deleteObject(folder.getPath());
        folder.setPath(newFolderPath);

        return getFileResponseDtoByFileInfos(updatedFileInfo);
    }

    private List<FileInfo> remoteFilesInBucket(List<FileInfo> filesInfo) {
        return filesInfo.stream().map(fi -> {
            String newFilePath = getNewPath(fi.getPath());
            dataStorageService.moveFile(fi.getPath(), newFilePath);
            fi.setPath(newFilePath);
            return fi;
        }).toList();
    }

    private String getNewPath(String tempFilePath) {
        String normalizedTempFilePath = toUnixStylePath(tempFilePath);
        String basePath = normalizedTempFilePath.substring(normalizedTempFilePath.indexOf("/"));
        return dirPrefix + basePath;
    }

    private void checkUserRights(Folder folder, User user) {
        if (user.getRole().equals(Role.ROLE_ADMIN)) {
            return;
        }
        if (!folder.getOwner().equals(user)) {
            throw new UserDoesntHaveRightException(user.getEmail());
        }
    }

    private String getFolderPathByFilePath(String filePath) {
        String normalizedPath = toUnixStylePath(filePath);
        return normalizedPath.substring(0, normalizedPath.lastIndexOf("/"));
    }
}
