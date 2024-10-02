package compress.data_keeper.services;

import compress.data_keeper.domain.Folder;
import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.folders.FolderDto;
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
    public String createFolder(FolderDto dto, User user) {

        Folder folder;
        if (dto == null) {
            folder = Folder.builder()
                    .name(LocalDateTime.now().toString())
                    .owner(user)
                    .build();

        } else if (dto.getPath() != null && !dto.getPath().isBlank()) {
            return dto.getPath();

        } else {
            folder = Folder.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .owner(user)
                    .build();
        }

        Folder savedFolder = folderRepository.save(folder);

        String folderPath = createFolderPath(savedFolder.getId().toString(), savedFolder.getOwner().getId());

        savedFolder.setPath(folderPath);

        return folderPath;
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
