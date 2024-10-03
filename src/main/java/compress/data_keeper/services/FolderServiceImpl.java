package compress.data_keeper.services;

import compress.data_keeper.domain.Folder;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.exception_handler.not_found.exceptions.FolderNotFoundException;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.repository.FolderRepository;
import compress.data_keeper.services.interfaces.FolderService;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${prefix.dir}")
    private String dirPrefix;

    private final MinioClient minioClient;

    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public Folder getFolder(FolderDto dto, User user) {

        if (dto == null) {
            return createFolder(user);
        } else {
            String folderPath = dto.getPath();
            if (folderPath != null && !folderPath.isBlank()) {
                return getFolderByPath(folderPath);
            } else {
                return createFolder(dto, user);
            }
        }
    }

    private Folder getFolderByPath(String path) {
        return folderRepository.findByPath(path)
                .orElseThrow(() -> new FolderNotFoundException(path));
    }

    private Folder createFolder(User user) {
        Folder folder = Folder.builder()
                .name(LocalDateTime.now().toString())
                .owner(user)
                .build();

        return createFolder(folder);
    }

    private Folder createFolder(FolderDto dto, User user) {
        String dtoFolderName = dto.getName();
        String folderName = dtoFolderName == null || dtoFolderName.isBlank() ?
                LocalDateTime.now().toString() : dtoFolderName;

        Folder folder = Folder.builder()
                .name(folderName)
                .description(dto.getDescription())
                .owner(user)
                .build();

        return createFolder(folder);
    }

    private Folder createFolder(Folder folder) {

        Folder savedFolder = folderRepository.save(folder);
        String folderPath = createFolderPath(savedFolder.getId().toString(), savedFolder.getOwner().getId());

        savedFolder.setPath(folderPath);

        return savedFolder;
    }

    private String createFolderPath(String folderUUID, Long userId) {

        String path = Path.of(dirPrefix, userId.toString(), folderUUID).toString();

        try {
            ObjectWriteResponse createdFolder = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(
                                    new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build());
            return createdFolder.object();
        } catch (Exception e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
