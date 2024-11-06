package compress.data_keeper.services;

import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.repository.FileInfoRepository;
import compress.data_keeper.services.mapping.FileInfoMapperService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
class FileServiceImplTest {

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private FileInfoMapperService fileInfoMapperService;

    @InjectMocks
    private FileServiceImpl fileService;

    @Nested
    @DisplayName("Tests for createFileInfo method")
    class CreateFileInfo_test {

        @Test
        public void createFileInfo_with_correct_date() throws Exception {

            Folder folder1 = Folder.builder()
                    .path("/test1/test1/test1")
                    .build();

            Folder folder2 = Folder.builder()
                    .path("/test2/test2/test2")
                    .build();

            FileInfoDto dto1 = new FileInfoDto();

            FileInfoDto dto2 = new FileInfoDto();

            List<FileInfoDto> dtos = List.of(dto1, dto2);

            UUID file1Id = UUID.randomUUID();
            FileInfo fileInfo1 = FileInfo.builder()
                    .id(file1Id)
                    .name("file1.txt")
                    .type(MediaType.TEXT_PLAIN_VALUE)
                    .folder(folder1)
                    .description("test1")
                    .isOriginalFile(true)
                    .build();

            UUID file2Id = UUID.randomUUID();
            FileInfo fileInfo2 = FileInfo.builder()
                    .id(file2Id)
                    .name("file2.txt")
                    .type(MediaType.TEXT_PLAIN_VALUE)
                    .folder(folder2)
                    .description("test2")
                    .isOriginalFile(true)
                    .build();

            when(fileInfoMapperService.toFileInfo(dto1)).thenReturn(fileInfo1);
            when(fileInfoMapperService.toFileInfo(dto2)).thenReturn(fileInfo2);
            when(fileInfoRepository.saveAll(any())).thenReturn(List.of(fileInfo1, fileInfo2));

            List<FileInfo> createdFileInfos = fileService.createFileInfo(dtos);

            assertEquals(2, createdFileInfos.size());
            assertEquals(file1Id, createdFileInfos.get(0).getId());
            assertEquals(file2Id, createdFileInfos.get(1).getId());

            ArgumentCaptor<List<FileInfo>> argumentCaptor = ArgumentCaptor.forClass(List.class);
            verify(fileInfoRepository).saveAll(argumentCaptor.capture());
            assertEquals(2, argumentCaptor.getValue().size());
        }
    }
}