package compress.data_keeper.scheduler;

import compress.data_keeper.domain.entity.EntityInfo;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class TemporaryFilesCleaner {

    @Value("${bucket.temp}")
    private String tempBucketName;

    @Value("${scheduler.interval}")
    private long interval;

    private final FileService fileService;

    private final DataStorageService dataStorageService;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanTemporaryFiles() {
        List<FileInfo> filesInfos = fileService.findOldTempFiles(tempBucketName, interval);
        List<String> filesPaths = filesInfos.stream()
                .map(EntityInfo::getPath)
                .toList();
        dataStorageService.deleteObjectsFromBucket(tempBucketName, filesPaths);
        fileService.deleteFilesInfos(filesInfos);
    }
}
