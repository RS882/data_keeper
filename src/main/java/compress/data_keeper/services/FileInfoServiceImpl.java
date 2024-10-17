package compress.data_keeper.services;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.repository.FileInfoRepository;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.mapping.FileInfoMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static compress.data_keeper.services.utilities.FileUtilities.getFileExtension;

@Service
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {

    private final FileInfoRepository fileInfoRepository;

    private final FileInfoMapperService fileInfoMapperService;

    @Override
    @Transactional
    public FileInfo createFileInfo(FileInfoDto dto) {
        List<FileInfo> fileInfos = createFileInfo(Collections.singletonList(dto));
        return fileInfos.get(0);
    }

    @Override
    @Transactional
    public List<FileInfo> createFileInfo(List<FileInfoDto> dtos) {
        List<FileInfo> fileInfos = dtos.stream()
                .map(fileInfoMapperService::toFileInfo)
                .toList();

        List<FileInfo> createdFileInfos = fileInfoRepository.saveAll(fileInfos);

        createdFileInfos.forEach(fi -> {
            Path outputFilePath = Path.of(fi.getFolder().getPath(),
                    fi.getId() + getFileExtension(fi.getName()));
            fi.setPath(outputFilePath.toString());
        });

        return createdFileInfos;
    }

    @Override
    @Transactional
    public List<FileInfo> getFileInfoByFolderId(UUID folderId) {
        return fileInfoRepository.findByFolderId(folderId);
    }

    @Override
    public List<FileInfo> updateFileInfos(List<FileInfo> fileInfos) {
        return fileInfoRepository.saveAll(fileInfos);
    }
}
