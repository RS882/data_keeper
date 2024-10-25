package compress.data_keeper.repository;

import compress.data_keeper.domain.entity.FileInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileInfoRepository extends JpaRepository<FileInfo, UUID> {

    List<FileInfo> findByFolderId(UUID folderId);

    void deleteAllByFolderId(UUID folderId);

    Optional<FileInfo> findByIdAndIsOriginalFileTrue(UUID id);

    Page<FileInfo> findAllByIsOriginalFileTrue(Pageable pageable);

    @Query("SELECT f FROM FileInfo f JOIN f.folder fol WHERE fol.owner.id = :userId AND f.isOriginalFile = true")
    Page<FileInfo> findOriginalFilesByUserId(@Param("userId") Long userId, Pageable pageable);
}
