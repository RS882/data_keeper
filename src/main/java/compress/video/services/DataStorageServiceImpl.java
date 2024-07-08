package compress.video.services;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import compress.video.constants.MediaFormats;
import compress.video.services.interfaces.DataStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

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
    public void uploadFIle(String objectFile, String outputFile) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        checkAndCreateBucket(bucketName);

        minioClient.uploadObject(
                UploadObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(toUnixStylePath(outputFile))
                        .filename(objectFile)
                        .build()
        );
    }

    @Override
    public void uploadFIle(InputStream inputStream, String outputFile) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        checkAndCreateBucket(bucketName);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(toUnixStylePath(outputFile))
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType("image/" + IMAGE_FORMAT)
                        .build()
        );
    }

    @Override
    public void checkAndCreateBucket(String bucketName)
            throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        boolean found = minioClient
                .bucketExists(
                        BucketExistsArgs
                                .builder()
                                .bucket(bucketName)
                                .build());
        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(bucketName)
                            .objectLock(true)
                            .build());

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
            builder.append("        \"arn:aws:s3:::video\"\n");
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
            builder.append("        \"arn:aws:s3:::video/*/").append(prefixPublic).append("*\"\n");
            builder.append("      ]\n");
            builder.append("    }\n");
            builder.append("  ]\n");
            builder.append("}");

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs
                            .builder()
                            .bucket(bucketName)
                            .config(builder.toString()).build());
        }
    }

    @Override
    public String getTempFullPath(String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(toUnixStylePath(path))
                        .expiry(urlLifeTime, TimeUnit.DAYS)
                        .build());
    }

    private String toUnixStylePath(String path) {
        return path.replace("\\", "/");
    }
}


