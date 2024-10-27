package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
import compress.data_keeper.domain.entity.FileInfo;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.Map;

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
    @Mapping(target ="id", ignore = true)
    @Mapping(target ="createdAt", ignore = true)
    @Mapping(target ="updatedAt", ignore = true)
    public abstract FileInfo mapCommonFields(FileInfoDto dto);

    protected String getHash(FileInfoDto dto) {
        return calculateHash(dto.getInputStream());
    }

    @InheritConfiguration(name = "mapCommonFields")
    public abstract FileInfo toFileInfo(FileInfoDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @InheritConfiguration(name = "mapCommonFields")
    public abstract void updateFileInfo(FileInfoDto dto, @MappingTarget FileInfo currentFileInfo);

    @Mapping(target = "files", ignore = true)
    @Mapping(target = "pageNumber", source = "fileInfoPage.number")
    @Mapping(target = "pageSize", source = "fileInfoPage.size")
    @Mapping(target = "totalPages", source = "fileInfoPage.totalPages")
    @Mapping(target = "totalElements", source = "fileInfoPage.totalElements")
    @Mapping(target = "isFirstPage", source = "fileInfoPage.first")
    @Mapping(target = "isLastPage", source = "fileInfoPage.last")
    public abstract FileResponseDtoWithPagination toFileResponseDtoWithPagination(Page<FileInfo> fileInfoPage);

    @Mapping(source = "fileInfo.id", target = "fileId")
    @Mapping(source = "fileInfo.name", target = "fileName")
    @Mapping(source = "fileInfo.description", target = "fileDescription")
    @Mapping(source = "fileInfo.folder.name", target = "folderName")
    @Mapping(source = "fileInfo.folder.description", target = "folderDescription")
    public abstract FileResponseDto toDto(FileInfo fileInfo);
}
