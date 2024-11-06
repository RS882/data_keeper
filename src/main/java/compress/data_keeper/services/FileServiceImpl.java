package compress.data_keeper.services;

import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.*;
import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.domain.entity.EntityInfo;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileBucketName;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.BadFileExtensionException;
import compress.data_keeper.exception_handler.not_found.exceptions.FileInFolderNotFoundException;
import compress.data_keeper.exception_handler.not_found.exceptions.FileInfoNotFoundException;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.repository.FileInfoRepository;
import compress.data_keeper.services.file_action_servieces.interfaces.FileActionService;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.FileInfoMapperService;
import compress.data_keeper.services.mapping.FolderDtoMapperService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.domain.CustomMultipartFile.toCustomMultipartFile;
import static compress.data_keeper.services.utilities.FileActionUtilities.getFileActionServiceByContentType;
import static compress.data_keeper.services.utilities.FileUtilities.*;
import static compress.data_keeper.services.utilities.UserRightUtilities.checkUserRights;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DataStorageService dataStorageService;

    private final FolderService folderService;

    private final FolderDtoMapperService folderDtoMapperService;

    private final FileInfoMapperService fileInfoMapperService;

    private final FileInfoRepository fileInfoRepository;

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Value("${prefix.dir}")
    private String dirPrefix;

    @Value("${data.temp-folder}")
    private String tempFolder;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${bucket.temp}")
    private String tempBucketName;

    @Override
    @Transactional
    public FileResponseDto uploadFileTemporary(FileCreationDto fileCreationDto, User user) {
        MultipartFile file = toCustomMultipartFile(fileCreationDto.getFile());

        checkFile(file);

        FolderDto newFolderDto = folderDtoMapperService.toDto(fileCreationDto);
        newFolderDto.setBucketName(tempBucketName);

        final Folder folderForFile = folderService.createNewFolder(newFolderDto, user, tempFolder);

        FileInfoDto fileInfoDto = new FileInfoDto(file, folderForFile, fileCreationDto.getFileDescription());
        fileInfoDto.setIsOriginalFile(true);
        fileInfoDto.setBucketName(tempBucketName);
        final FileInfo fileInfo = createFileInfo(fileInfoDto);

        dataStorageService.uploadFIle(file, fileInfo.getPath()).object();

        List<FileInfo> filesInfos = new ArrayList<>();
        filesInfos.add(fileInfo);

        List<FileInfo> imgFilesInfos = createImagesForFile(
                file,
                folderForFile,
                fileInfo.getId());

        filesInfos.addAll(imgFilesInfos);

        return getFileResponseDtoByFileInfos(filesInfos, tempBucketName);
    }

    private FileResponseDto getFileResponseDtoByFileInfos(List<FileInfo> fileInfos, String bucketName) {
        Map<String, String> paths = new HashMap<>();
        fileInfos.forEach(fi -> {
            String key = fi.getIsOriginalFile() ? ORIGINAL_FILE_KEY : fi.getDescription();
            String value = fi.getPath();
            paths.put(key, value);
        });
        Map<String, String> links = getTempFileLinks(paths, bucketName);
        long linkLifeTimeDuration = timeUnitForTempLink.toMillis(urlLifeTime);
        ZonedDateTime linkIsValidUntil = ZonedDateTime.now()
                .plus(linkLifeTimeDuration, ChronoUnit.MILLIS);

        FileInfo originalFileInfo = fileInfos.stream()
                .filter(FileInfo::getIsOriginalFile)
                .findFirst()
                .orElse(null);

        FileResponseDto responseDto = originalFileInfo != null
                ? fileInfoMapperService.toDto(originalFileInfo)
                : new FileResponseDto();

        responseDto.setLinksToFiles(links);
        responseDto.setLinksIsValidUntil(linkIsValidUntil);

        return responseDto;
    }

    @Transactional
    protected List<FileInfo> createImagesForFile(
            MultipartFile file,
            Folder folder,
            UUID fileUUID) {

        final String imgFileName = fileUUID + "." + IMAGE_FORMAT;

        final FileActionService fileActionService = getFileActionService(file);

        List<FileInfoDto> fileInfoDtoList = new ArrayList<>();

        if (fileActionService != null) {
            Map<String, InputStream> imgFileStreams = fileActionService.getFileImages(file);
            imgFileStreams.forEach((key, value) -> {
                Path filePath = Path.of(folder.getPath(), key, imgFileName);
                InputStreamDto dto = new InputStreamDto(
                        value,
                        key + "." + IMAGE_FORMAT,
                        MediaType.IMAGE_JPEG_VALUE);
                String imgFilePath = dataStorageService.uploadFIle(dto, filePath.toString()).object();

                FileInfoDto fileInfoDto = new FileInfoDto(value, folder, imgFilePath, key);
                fileInfoDto.setBucketName(tempBucketName);
                fileInfoDtoList.add(fileInfoDto);
            });
            return createFileInfo(fileInfoDtoList);
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

    private Map<String, String> getTempFileLinks(Map<String, String> paths, String tempBucketName) {
        Map<String, String> links = new HashMap<>();
        paths.forEach((key, value) ->
                links.put(key, dataStorageService.getTempLink(value, tempBucketName))
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
        UUID originalFileId = dto.getFileId();
        FileInfo tempFileInfo = findOriginalFileInfoById(originalFileId);

        if (!tempFileInfo.getBucketName().equals(tempBucketName)) {
            throw new BadFileBucketName(
                    String.format("File with id <%s> isn't in the temporary bucket",
                            originalFileId)
            );
        }
        Folder folder = tempFileInfo.getFolder();

        checkUserRights(folder, user);

        List<FileInfo> fileInfos = findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), originalFileId);
        if (fileInfos.isEmpty()) {
            throw new FileInFolderNotFoundException(folder.getId());
        }
        List<FileInfo> updatedFileInfo = remoteFilesInBucket(fileInfos);

        String newFolderPath = getNewPath(folder.getPath());
        dataStorageService.deleteObject(folder.getPath());
        folder.setPath(newFolderPath);
        folder.setBucketName(bucketName);
        folder.setTemp(false);

        return getFileResponseDtoByFileInfos(updatedFileInfo, bucketName);
    }

    private List<FileInfo> remoteFilesInBucket(List<FileInfo> filesInfo) {
        return filesInfo.stream().map(fi -> {
            String newFilePath = getNewPath(fi.getPath());
            dataStorageService.moveFile(fi.getPath(), newFilePath);
            fi.setPath(newFilePath);
            fi.setBucketName(bucketName);
            return fi;
        }).toList();
    }

    private String getNewPath(String tempFilePath) {
        String normalizedTempFilePath = toUnixStylePath(tempFilePath);
        String basePath = normalizedTempFilePath.substring(normalizedTempFilePath.indexOf("/"));
        return dirPrefix + basePath;
    }

    @Override
    public FileResponseDtoWithPagination findAllFiles(Pageable pageable) {
        Page<FileInfo> filesInfos = fileInfoRepository.findAllByIsOriginalFileTrue(pageable);
        return getFileResponseDtoWithPagination(filesInfos);
    }

    @Override
    public FileResponseDtoWithPagination findFilesByUserId(Long userId, User currentUser, Pageable pageable) {
        checkUserRights(userId, currentUser);
        Page<FileInfo> filesInfos = fileInfoRepository.findOriginalFilesByUserId(userId, pageable);
        return getFileResponseDtoWithPagination(filesInfos);
    }

    @Override
    public FileResponseDto findFileByFileId(UUID fileId, User currentUser) {
        FileInfo fileInfo = findOriginalFileInfoById(fileId);
        Folder fileFolder = fileInfo.getFolder();
        checkUserRights(fileFolder, currentUser);

        List<FileInfo> fileInfos = findFilesInfosByFolderIdAndOriginalFileId(fileFolder.getId(), fileId);
        return getFileResponseDtoByFileInfos(fileInfos, fileInfo.getBucketName());
    }

    @Override
    @Transactional
    public FileResponseDto updateFileInfo(FileUpdateDto fileUpdateDto, User currentUser) {
        UUID fileId = fileUpdateDto.getFileId();
        FileInfo fileInfo = findOriginalFileInfoById(fileId);
        Folder fileFolder = fileInfo.getFolder();
        checkUserRights(fileFolder, currentUser);
        if (fileUpdateDto.getFileName() != null) {
            checkFileExtension(fileUpdateDto.getFileName(), fileInfo.getName());
        }
        updateFolderInformation(fileFolder, fileUpdateDto);
        updateFileInformation(fileInfo, fileUpdateDto);

        List<FileInfo> fileInfos = findFilesInfosByFolderIdAndOriginalFileId(fileFolder.getId(), fileId);
        return getFileResponseDtoByFileInfos(fileInfos, fileInfo.getBucketName());
    }

    @Override
    @Transactional
    public FileInfo updateFileInfo(UUID fileId, FileInfoDto dto) {
        FileInfo fileInfo = findOriginalFileInfoById(fileId);
        fileInfoMapperService.updateFileInfo(dto, fileInfo);
        return fileInfo;
    }

    private void checkFileExtension(String newFileName, String currentFileName) {
        String ex1 = getFileExtension(newFileName);
        String ex2 = getFileExtension(currentFileName);
        if (!ex1.equalsIgnoreCase(ex2)) {
            throw new BadFileExtensionException(getFileExtension(newFileName));
        }
    }

    private void updateFileInformation(FileInfo fileInfo, FileUpdateDto dto) {
        if (dto.getFileName() != null) {
            fileInfo.setName(dto.getFileName());
            dataStorageService.updateOriginalFileName(
                    fileInfo.getBucketName(),
                    fileInfo.getPath(),
                    dto.getFileName());
        }
        if (dto.getFileDescription() != null) {
            fileInfo.setDescription(dto.getFileDescription());
        }
    }

    private void updateFolderInformation(Folder folder, FileUpdateDto dto) {
        if (dto.getFolderName() != null) {
            folder.setName(dto.getFolderName());
        }
        if (dto.getFolderDescription() != null) {
            folder.setDescription(dto.getFolderDescription());
        }
        if (dto.getIsFolderProtected() != null) {
            folder.setProtected(dto.getIsFolderProtected());
        }
    }

    private FileResponseDtoWithPagination getFileResponseDtoWithPagination(Page<FileInfo> filesInfos) {
        FileResponseDtoWithPagination responseDto =
                fileInfoMapperService.toFileResponseDtoWithPagination(filesInfos);
        Set<FileResponseDto> fileResponseDtoSet = filesInfos.getContent().stream()
                .map(fi -> {
                    Folder folder = fi.getFolder();
                    List<FileInfo> fileInfos = findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), fi.getId());
                    return getFileResponseDtoByFileInfos(fileInfos, folder.getBucketName());
                }).collect(Collectors.toSet());
        responseDto.setFiles(fileResponseDtoSet);
        return responseDto;
    }

    @Override
    @Transactional
    public FileInfo createFileInfo(FileInfoDto dto) {
        List<FileInfo> fileInfos = createFileInfo(Collections.singletonList(dto));
        return fileInfos.get(0);
    }

    @Override
    @Transactional
    public List<FileInfo> createFileInfo(List<FileInfoDto> dtos) {
        List<FileInfo> fileInfos = dtos.stream()
                .map(fileInfoMapperService::toFileInfo)
                .toList();
        List<FileInfo> createdFileInfos = fileInfoRepository.saveAll(fileInfos);
        createdFileInfos.forEach(fi -> {
            if (fi.getPath() == null || fi.getPath().isBlank()) {
                Path outputFilePath = Path.of(
                        fi.getFolder().getPath(),
                        fi.getId() + getFileExtension(fi.getName())
                );
                fi.setPath(toUnixStylePath(outputFilePath.toString()));
            }
        });
        return createdFileInfos;
    }

    @Override
    public FileInfo findOriginalFileInfoById(UUID id) {
        return fileInfoRepository.findByIdAndIsOriginalFileTrue(id)
                .orElseThrow(() -> new FileInfoNotFoundException(id));
    }

    @Override
    public List<FileInfo> findFilesInfosByFolderIdAndOriginalFileId(UUID folderId, UUID fileId) {
        return fileInfoRepository.findByFolderIdAndPathContainsFileId(folderId, fileId);
    }

    @Override
    public void deleteAllFileInfosByFolderId(UUID folderId) {
        fileInfoRepository.deleteAllByFolderId(folderId);
    }

    @Override
    @Transactional
    public void deleteFileById(UUID id, User currentUser) {
        FileInfo fileInfo = findOriginalFileInfoById(id);
        Folder fileFolder = fileInfo.getFolder();
        checkUserRights(fileFolder, currentUser);

        List<FileInfo> fileInfos = findFilesInfosByFolderIdAndOriginalFileId(fileFolder.getId(), id);
        fileInfoRepository.deleteAll(fileInfos);
        List<String> filesPaths = fileInfos.stream().map(EntityInfo::getPath).toList();
        dataStorageService.deleteObjectsFromBucket(fileInfo.getBucketName(), filesPaths);
    }

    @Override
    public void deleteFilesInfos(List<FileInfo> fileInfos) {
        fileInfoRepository.deleteAll(fileInfos);
    }

    @Override
    public List<FileInfo> findOldTempFiles(String bucketName, long secondsInterval) {
         return fileInfoRepository.findOldTempFilesInfos(
                 bucketName,
                 LocalDateTime.now().minusSeconds(secondsInterval));
    }
}
