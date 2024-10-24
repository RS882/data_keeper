package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.entity.FileInfo;
import org.mapstruct.*;

import static compress.data_keeper.services.utilities.FileCalculators.calculateHash;

@Mapper
public abstract class FileInfoMapperService {

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "description", source = "fileDescription")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "folder", source = "fileFolder")
    @Mapping(target = "path", source = "filePath")
    @Mapping(target = "bucketName", source = "bucketName")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "hash", expression = "java(getHash(dto))")
    public abstract FileInfo mapCommonFields(FileInfoDto dto);

    @InheritConfiguration(name = "mapCommonFields")
    public abstract FileInfo toFileInfo(FileInfoDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @InheritConfiguration(name = "mapCommonFields")
    public abstract void updateFileInfo(FileInfoDto dto, @MappingTarget FileInfo currentFileInfo);

    protected String getHash(FileInfoDto dto) {
        return calculateHash(dto.getInputStream());
    }
}
