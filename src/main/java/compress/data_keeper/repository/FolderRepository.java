package compress.data_keeper.repository;

import compress.data_keeper.domain.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    Optional<Folder> findByPath(String path);
}
