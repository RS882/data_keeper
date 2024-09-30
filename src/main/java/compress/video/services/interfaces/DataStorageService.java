package compress.video.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DataStorageService {
    void uploadFIle(String objectFile, String outputFile);

    void uploadFIle(InputStream inputStream, String outputFile);

    void uploadFIle(MultipartFile multipartFile, String outputFile);

    void checkAndCreateBucket(String bucketName);

    String getTempFullPath(String path);
}
