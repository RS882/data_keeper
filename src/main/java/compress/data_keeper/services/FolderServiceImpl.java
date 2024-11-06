package compress.data_keeper.services;

import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.not_found.exceptions.FolderNotFoundException;
import compress.data_keeper.repository.FolderRepository;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static compress.data_keeper.services.utilities.FileUtilities.toUnixStylePath;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final DataStorageService dataStorageService;

    private final FolderRepository folderRepository;

    private String folderPrefix;

    public static final String DEFAULT_FOLDER_PREFIX_NAME = "Folder created in : ";

    @Override
    @Transactional
    public Folder createNewFolder(FolderDto dto, User user, String dirPrefix) {
        folderPrefix = dirPrefix;
        if (dto == null) {
            return createFolder(user);
        } else {
            String folderPath = dto.getPath();
            if (folderPath != null && !folderPath.isBlank()) {
                return getFolderByFolderPath(folderPath);
            } else {
                return createFolder(dto, user);
            }
        }
    }

    @Override
    @Transactional
    public Folder getFolderByFolderPath(String folderPath) {
        return folderRepository.findByPath(toUnixStylePath(folderPath))
                .orElseThrow(() -> new FolderNotFoundException(folderPath));
    }

    private Folder createFolder(User user) {
        Folder folder = Folder.builder()
                .name(getDefaultFolderName())
                .owner(user)
                .build();
        return createFolder(folder);
    }

    private Folder createFolder(FolderDto dto, User user) {
        String dtoFolderName = dto.getName();
        String folderName = dtoFolderName == null || dtoFolderName.isBlank() ?
                getDefaultFolderName() : dtoFolderName;

        Folder folder = Folder.builder()
                .name(folderName)
                .description(dto.getDescription())
                .bucketName(dto.getBucketName())
                .isProtected(dto.isFolderProtected())
                .owner(user)
                .build();
        return createFolder(folder);
    }

    private Folder createFolder(Folder folder) {
        Folder savedFolder = folderRepository.save(folder);
        String folderPath = dataStorageService.createFolderPath(
                savedFolder.getId().toString(),
                savedFolder.getOwner().getId(),
                folderPrefix
        );
        savedFolder.setPath(folderPath);
        savedFolder.setTemp(true);
        return savedFolder;
    }

    private String getDefaultFolderName() {
        return DEFAULT_FOLDER_PREFIX_NAME + LocalDateTime.now();
    }
}
