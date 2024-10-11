package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.entity.FileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static compress.data_keeper.services.utilities.FileCalculators.calculateHash;

@Mapper
public abstract class FileInfoMapperService {

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "description", source = "fileDescription")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "folder", source = "fileFolder")
    @Mapping(target = "path", source = "filePath")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "hash", expression = "java(getHash(dto))")
    public abstract FileInfo toFileInfo(FileInfoDto dto);

    protected String getHash(FileInfoDto dto) {
        return calculateHash(dto.getInputStream());
    }
}
