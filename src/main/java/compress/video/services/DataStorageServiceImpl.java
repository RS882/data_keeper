package compress.video.services;

import compress.video.constants.MediaFormats;
import compress.video.exception_handler.server_exception.ServerIOException;
import compress.video.services.interfaces.DataStorageService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static compress.video.services.DataStorageServiceImpl.InputStreamDto.getInputStreamDto;

@Service
@RequiredArgsConstructor
public class DataStorageServiceImpl implements DataStorageService, MediaFormats {

    private final MinioClient minioClient;

    @Value("${lifetime.url}")
    private int urlLifeTime;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${prefix.public}")
    private String prefixPublic;


    @Override
    public void uploadFIle(String objectFile, String outputFile) {

        checkAndCreateBucket();

        try {
            minioClient.uploadObject(
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
    public void uploadFIle(InputStream inputStream, String outputFile) {

        InputStreamDto dto = getInputStreamDto(inputStream, "image/" + IMAGE_FORMAT);

        uploadFIle(dto, outputFile);
    }

    @Override
    public void uploadFIle(MultipartFile file, String outputFile) {

        String contentType = file.getContentType() != null ?
                file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        InputStreamDto dto = getInputStreamDto(file, contentType);

        uploadFIle(dto, outputFile);
    }


    private void uploadFIle(InputStreamDto inputStreamDto, String outputFile) {

        checkAndCreateBucket();

        InputStream fileInputStream = inputStreamDto.getInputStream();
        String contentType = inputStreamDto.getContentType();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toUnixStylePath(outputFile))
                            .stream(fileInputStream, fileInputStream.available(), -1)
                            .contentType(contentType)
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
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(toUnixStylePath(path))
                            .expiry(urlLifeTime, TimeUnit.DAYS)
                            .build());
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

    @Getter
    @AllArgsConstructor
    public static class InputStreamDto {

        InputStream inputStream;

        String contentType;

        static InputStreamDto getInputStreamDto(MultipartFile file, String contentType) {
            try {
                return getInputStreamDto(file.getInputStream(), contentType);
            } catch (IOException e) {
                throw new ServerIOException(e.getMessage());
            }
        }

        static InputStreamDto getInputStreamDto(InputStream inputStream, String contentType) {
            return new InputStreamDto(inputStream, contentType);
        }
    }
}


