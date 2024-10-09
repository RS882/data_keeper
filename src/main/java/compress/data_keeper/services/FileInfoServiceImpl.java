package compress.data_keeper.services;

import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.exception_handler.server_exception.ServerIOException;
import compress.data_keeper.repository.FileInfoRepository;
import compress.data_keeper.services.interfaces.FileInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static compress.data_keeper.services.utilities.FileHashCalculator.calculateHash;
import static compress.data_keeper.services.utilities.FileUtilities.getFileExtension;

@Service
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {

    private final FileInfoRepository fileInfoRepository;

    @Override
    @Transactional
    public FileInfo createFileInfo(MultipartFile file, Folder folder, String fileDescription) {

        try {
            FileInfo fileInfo = FileInfo.builder()
                    .name(file.getOriginalFilename())
                    .description(fileDescription)
                    .size(file.getSize())
                    .type(file.getContentType())
                    .folder(folder)
                    .hash(calculateHash(file.getInputStream()))
                    .build();

            FileInfo createdFileInfo = fileInfoRepository.save(fileInfo);

            Path outputFilePath = Path.of(folder.getPath(), createdFileInfo.getId() + getFileExtension(file));

            createdFileInfo.setPath(outputFilePath.toString());

            return fileInfo;

        } catch (IOException e) {
            throw new ServerIOException(e.getMessage());
        }
    }
}
