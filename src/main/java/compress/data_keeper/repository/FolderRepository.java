package compress.data_keeper.repository;

import compress.data_keeper.domain.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    Optional<Folder> findByPath(String path);

    @Modifying
    @Transactional
    @Query("DELETE FROM Folder f WHERE f.fileInfoSet IS EMPTY" +
            " AND (f.isTemp = true OR (f.isTemp = false AND f.isProtected = false))")
    void deleteAllEmpty();
}
