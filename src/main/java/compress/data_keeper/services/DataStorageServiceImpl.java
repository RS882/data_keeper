package compress.data_keeper.services;

import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.interfaces.DataStorageService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.FileMetaDataConstants.*;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;
import static compress.data_keeper.domain.dto.InputStreamDto.getInputStreamDto;
import static compress.data_keeper.services.utilities.FileUtilities.toUnixStylePath;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataStorageServiceImpl implements DataStorageService {

    private final MinioClient minioClient;

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Value("${bucket.name}")
    private String newBucketName;

    @Value("${bucket.temp}")
    private String tempBucketName;


    @Override
    public ObjectWriteResponse uploadFIle(String objectFile, String outputFile) {
        checkAndCreateBucket();
        try {
            return minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket(tempBucketName)
                            .object(toUnixStylePath(outputFile))
                            .filename(objectFile)
                            .build());
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    @Override
    public ObjectWriteResponse uploadFIle(InputStream inputStream, String outputFile, String originalFileName) {
        InputStreamDto dto = getInputStreamDto(inputStream, originalFileName, "image/" + IMAGE_FORMAT);
        return uploadFIle(dto, outputFile);
    }

    @Override
    public ObjectWriteResponse uploadFIle(MultipartFile file, String outputFile) {
        String fileContentType = file.getContentType();
        String contentType = fileContentType == null || fileContentType.isEmpty() ?
                MediaType.APPLICATION_OCTET_STREAM_VALUE : fileContentType;
        InputStreamDto dto = getInputStreamDto(file, contentType);
        return uploadFIle(dto, outputFile);
    }

    @Override
    public ObjectWriteResponse uploadFIle(InputStreamDto inputStreamDto, String outputFile) {
        checkAndCreateBucket();
        InputStream fileInputStream = inputStreamDto.getInputStream();
        Map<String, String> metaData = new HashMap<>();
        metaData.put(USER_METADATA_PREFIX + ORIGINAL_FILENAME, inputStreamDto.getOriginalFilename());
        try {
            return minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(tempBucketName)
                            .object(toUnixStylePath(outputFile))
                            .stream(fileInputStream, fileInputStream.available(), -1)
                            .contentType(inputStreamDto.getContentType())
                            .userMetadata(metaData)
                            .build()
            );
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    @Override
    public void updateObjectMetadata(String bucketName, String objectName, Map<String, String> newMetaData) {
        try {
            InputStream objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(objectStream, objectStream.available(), -1)
                            .userMetadata(newMetaData)
                            .build()
            );
            objectStream.close();
        } catch (Exception e) {
            throw new ServerIOException("Error updating metadata: " + e.getMessage());
        }
    }

    @Override
    public void updateOriginalFileName(String bucketName, String objectName, String newFileName) {
        Map<String, String> metaData = new HashMap<>();
        metaData.put(USER_METADATA_PREFIX + ORIGINAL_FILENAME, newFileName);
        updateObjectMetadata(bucketName, objectName, metaData);
    }

    @Override
    public ObjectWriteResponse moveFile(String currentFilePath, String newFilePath) {
        checkAndCreateBucket(newBucketName, true);
        try {
            ObjectWriteResponse newObject = minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(newBucketName)
                            .object(newFilePath)
                            .source(
                                    CopySource.builder()
                                            .bucket(tempBucketName)
                                            .object(currentFilePath)
                                            .build())
                            .build());
            deleteObject(currentFilePath);
            return newObject;
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    private boolean checkBucket(String bucketName) {
        try {
            return minioClient
                    .bucketExists(
                            BucketExistsArgs
                                    .builder()
                                    .bucket(bucketName)
                                    .build());
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    @Override
    public void checkAndCreateBucket(String checkedBucketName, boolean isObjectLock) {
        try {
            boolean found = checkBucket(checkedBucketName);
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs
                                .builder()
                                .bucket(checkedBucketName)
                                .objectLock(isObjectLock)
                                .build());

                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs
                                .builder()
                                .bucket(checkedBucketName)
                                .config(createBucketPolicy(checkedBucketName))
                                .build());
            }
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    public void checkAndCreateBucket() {
        checkAndCreateBucket(tempBucketName, false);
    }

    @Override
    public String getTempLink(String path, String bucketName) {
        if (path == null) {
            return null;
        }
        Map<String, String> queryParams = getQueryParamsForFile(path, bucketName);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(toUnixStylePath(path))
                            .expiry(urlLifeTime, timeUnitForTempLink)
                            .extraQueryParams(queryParams)
                            .build());
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    public String getTempLink(String path) {
        return getTempLink(path, tempBucketName);
    }

    private Map<String, String> getQueryParamsForFile(String path, String bucketName) {
        Map<String, String> queryParams = new HashMap<>();
        String originalFileName = getFileUserMetaData(path, bucketName).get(ORIGINAL_FILENAME.toLowerCase());
        if (originalFileName != null || !originalFileName.isBlank()) {
            queryParams.put(RESPONSE_CONTENT_DISPOSITION, ATTACHMENT_FILENAME + originalFileName);
        }
        return queryParams;
    }

    @Override
    public Map<String, String> getFileUserMetaData(String path, String bucketName) {
        try {
            return minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(toUnixStylePath(path))
                                    .build())
                    .userMetadata();
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    private String createBucketPolicy(String checkedBucketName) {
        StringBuilder builder = new StringBuilder();

        builder.append("{\n");
        builder.append("  \"Version\": \"2012-10-17\",\n");
        builder.append("  \"Statement\": [\n");
        builder.append("    {\n");
        builder.append("      \"Effect\": \"Allow\",\n");
        builder.append("      \"Principal\": {\n");
        builder.append("        \"AWS\": [\n");
        builder.append("          \"*\"\n");
        builder.append("        ]\n");
        builder.append("      },\n");
        builder.append("      \"Action\": [\n");
        builder.append("        \"s3:GetBucketLocation\",\n");
        builder.append("        \"s3:ListBucket\"\n");
        builder.append("      ],\n");
        builder.append("      \"Resource\": [\n");
        builder.append("        \"arn:aws:s3:::").append(checkedBucketName).append("\"\n");
        builder.append("      ]\n");
        builder.append("    },\n");
        builder.append("    {\n");
        builder.append("      \"Effect\": \"Allow\",\n");
        builder.append("      \"Principal\": {\n");
        builder.append("        \"AWS\": [\n");
        builder.append("          \"*\"\n");
        builder.append("        ]\n");
        builder.append("      },\n");
        builder.append("      \"Action\": [\n");
        builder.append("        \"s3:GetObject\"\n");
        builder.append("      ],\n");
        builder.append("      \"Resource\": [\n");
        builder.append("        \"arn:aws:s3:::").append(checkedBucketName).append("/*/").append("public").append("*\"\n");
        builder.append("      ]\n");
        builder.append("    }\n");
        builder.append("  ]\n");
        builder.append("}");

        return builder.toString();
    }

    @Override
    public String createFolderPath(String folderUUID, Long userId, String folderPrefix) {

//        checkAndCreateBucket(bucketName);
//
        String path = Path.of(folderPrefix, userId.toString(), folderUUID).toString();
//
//        try {
//            ObjectWriteResponse createdFolder = minioClient.putObject(
//                    PutObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(toUnixStylePath(path))
//                            .stream(
//                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
//                            .build());
//
//            return toUnixStylePath(createdFolder.object());
        return toUnixStylePath(path);
//        } catch (Exception e) {
//            throw new ServerIOException(e.getMessage());
//        }
    }

    @Override
    public void deleteObject(String objectPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(tempBucketName)
                            .object(objectPath)
                            .build());
            log.info("Object deleted successful : {}", objectPath);
        } catch (Exception e) {
            throw new ServerIOException("Failed to delete file from Minio: " + objectPath);
        }
    }

    @Override
    public boolean isObjectExist(String objectPath, String bucketName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteObjectsFromBucket(String bucketName, List<String> objectsNames) {

        List<DeleteObject> objects = objectsNames.stream()
                .map(DeleteObject::new)
                .collect(Collectors.toList());
        try {
            Iterable<Result<DeleteError>> results =
                    minioClient.removeObjects(
                            RemoveObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .objects(objects)
                                    .build());
            log.info("Removed {} objects from the bucket {}", objects.size(), bucketName);
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("Error in deleting object {}; {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            throw new ServerIOException("Failed to delete file from Minio");
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        try {
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("Bucket '{}' deleted successfully", bucketName);
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }

    @Override
    public void clearAndDeleteBucket(String bucketName) {
        if (!checkBucket(bucketName)) return;
        Iterable<Result<Item>> objects = getAllObjectFromBucket(bucketName);
        List<String> objectsToDelete = StreamSupport.stream(objects.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get().objectName();
                    } catch (Exception e) {
                        throw new ServerIOException(e.getMessage());
                    }
                }).toList();
        deleteObjectsFromBucket(bucketName, objectsToDelete);
        deleteBucket(bucketName);
    }

    @Override
    public Iterable<Result<Item>> getAllObjectFromBucket(String bucketName) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build()
        );
    }
}