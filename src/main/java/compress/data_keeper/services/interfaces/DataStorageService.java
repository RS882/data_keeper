package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.InputStreamDto;
import io.minio.ObjectWriteResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

public interface DataStorageService {
    ObjectWriteResponse uploadFIle(String objectFile, String outputFile);

    ObjectWriteResponse uploadFIle(InputStream inputStream, String outputFile, String originalFileName);

    ObjectWriteResponse uploadFIle(MultipartFile multipartFile, String outputFile);

    ObjectWriteResponse uploadFIle(InputStreamDto inputStreamDto, String outputFile);

    ObjectWriteResponse moveFile( String currentFilePath, String newFilePath);

    void checkAndCreateBucket(String bucketName);

    String getTempLink(String path);

    Map<String, String> getFileUserMetaData(String path);

    String createFolderPath(String folderUUID, Long userId, String folderPrefix);

    void deleteObject(String objectPath);
}
