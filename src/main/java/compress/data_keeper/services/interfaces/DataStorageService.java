package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.InputStreamDto;
import io.minio.ObjectWriteResponse;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DataStorageService {

    void setBucketName(String bucketName);

    void setNewBucketName(String newBucketName);

    ObjectWriteResponse uploadFIle(String objectFile, String outputFile);

    ObjectWriteResponse uploadFIle(InputStream inputStream, String outputFile, String originalFileName);

    ObjectWriteResponse uploadFIle(MultipartFile multipartFile, String outputFile);

    ObjectWriteResponse uploadFIle(InputStreamDto inputStreamDto, String outputFile);

    ObjectWriteResponse moveFile(String currentFilePath, String newFilePath);

    void checkAndCreateBucket(String checkedBucketName, boolean isObjectLock);

    String getTempLink(String path, String bucketName);

    String getTempLink(String path);

    Map<String, String> getFileUserMetaData(String path, String bucketName);

    String createFolderPath(String folderUUID, Long userId, String folderPrefix);

    void deleteObject(String objectPath);

    boolean isObjectExist(String objectPath,String bucketName);

    void deleteObjectsFromBucket(String bucketName, List<String> objectsNames);

    void deleteBucket(String bucketName);

    void clearAndDeleteBucket(String bucketName);

    Iterable<Result<Item>> getAllObjectFromBucket(String bucketName);
}
