package compress.data_keeper.repository;

import compress.data_keeper.domain.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileInfoRepository extends JpaRepository<FileInfo, UUID> {

    List<FileInfo> findByFolderId(UUID folderId);
}
