package compress.data_keeper.repository;

import compress.data_keeper.domain.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileInfoRepository extends JpaRepository<FileInfo, UUID> {
}
