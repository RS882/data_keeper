package compress.data_keeper.services;

import compress.data_keeper.domain.dto.InputStreamDto;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.services.interfaces.DataStorageService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static compress.data_keeper.configs.MinioStorageConfig.timeUnitForTempLink;
import static compress.data_keeper.constants.MediaFormats.IMAGE_FORMAT;


import static compress.data_keeper.domain.dto.InputStreamDto.getInputStreamDto;
import static compress.data_keeper.services.utilities.FileMetaDataConstants.*;

@Service
@RequiredArgsConstructor
public class DataStorageServiceImpl implements DataStorageService {

    private final MinioClient minioClient;

    @Value("${url-lifetime}")
    private int urlLifeTime;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${prefix.public}")
    private String prefixPublic;


    @Override
    public ObjectWriteResponse uploadFIle(String objectFile, String outputFile) {

        checkAndCreateBucket();

        try {
            return minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket(bucketName)
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
                            .bucket(bucketName)
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
    public void checkAndCreateBucket(String checkedBucketName) {
        try {
            boolean found = minioClient
                    .bucketExists(
                            BucketExistsArgs
                                    .builder()
                                    .bucket(checkedBucketName)
                                    .build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs
                                .builder()
                                .bucket(checkedBucketName)
                                .objectLock(true)
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
        checkAndCreateBucket(bucketName);
    }

    @Override
    public String getTempFullPath(String path) {

        if (path == null) return null;

        Map<String, String> queryParams = getQueryParamsForFile(path);

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

    private Map<String, String> getQueryParamsForFile(String path) {

        Map<String, String> queryParams = new HashMap<>();

        String originalFileName = getFileUserMetaData(path).get(ORIGINAL_FILENAME.toLowerCase());

        if (originalFileName != null || !originalFileName.isBlank()) {
            queryParams.put(RESPONSE_CONTENT_DISPOSITION, ATTACHMENT_FILENAME + originalFileName);
        }
        return queryParams;
    }

    @Override
    public Map<String, String> getFileUserMetaData(String path) {
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

    private String toUnixStylePath(String path) {
        return path.replace("\\", "/");
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
        builder.append("        \"arn:aws:s3:::").append(checkedBucketName).append("/*/").append(prefixPublic).append("*\"\n");
        builder.append("      ]\n");
        builder.append("    }\n");
        builder.append("  ]\n");
        builder.append("}");

        return builder.toString();
    }
}


