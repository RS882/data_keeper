package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.dto.files.FileCreationDto;
import compress.data_keeper.domain.dto.folders.FolderDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public abstract class FolderDtoMapperService {

    @Mapping(target = "name", source = "folderName")
    @Mapping(target = "description", source = "folderDescription")
    @Mapping(target = "path", source = "folderPath")
    public abstract FolderDto toDto(FileCreationDto dto);
}

