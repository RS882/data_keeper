package compress.data_keeper.scheduler;

import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.services.interfaces.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class EmptyFolderCleaner {

    private final FolderService folderService;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteEmptyFolders() {
       folderService.deleteAllEmptyUnprotectedFolders();
    }
}
