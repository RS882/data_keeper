package compress.data_keeper.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.mapping.UserMapperService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Scheduler clean temp file integration tests ")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
@Transactional
@Rollback
class TemporaryFilesCleanerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemporaryFilesCleaner temporaryFilesCleaner;

    @Autowired
    private FileService fileService;

    @Autowired
    private DataStorageService dataStorageService;

    @Autowired
    private UserMapperService mapperService;

    @Value("${bucket.temp}")
    private String tempBucketName;

    @Value("${scheduler.interval}")
    private long interval;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String USER1_EMAIL = "Test1" + "@example.com";
    private static final String USER1_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_1 = "TestName1";

    private static final String USER2_EMAIL = "Test2" + "@example.com";
    private static final String USER2_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_2 = "TestName2";

    private static final String FILE_TEMP_LOAD_PATH = "/v1/file/temp";
    private static final String SAVE_TEMP_FILE_PATH = "/v1/file/save";

    private static final String LOGIN_URL = "/v1/auth/login";

    private String accessToken1;
    private Long currentUserId1;

    private String accessToken2;
    private Long currentUserId2;

    @BeforeEach
    void setUp() throws Exception {
        mapper.registerModule(new JavaTimeModule());
        loginUser1();
        loginUser2();
    }

    private void loginUser1() throws Exception {
        UserRegistrationDto dto1 = UserRegistrationDto
                .builder()
                .email(USER1_EMAIL)
                .userName(TEST_USER_NAME_1)
                .password(USER1_PASSWORD)
                .build();
        userRepository.save(mapperService.toEntity(dto1));
        String dtoJson1 = mapper.writeValueAsString(
                LoginDto.builder()
                        .email(USER1_EMAIL)
                        .password(USER1_PASSWORD)
                        .build());
        MvcResult result1 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson1))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse1 = result1.getResponse().getContentAsString();
        TokenResponseDto responseDto1 = mapper.readValue(jsonResponse1, TokenResponseDto.class);
        accessToken1 = responseDto1.getAccessToken();
        currentUserId1 = responseDto1.getUserId();
    }

    private void loginUser2() throws Exception {
        UserRegistrationDto dto2 = UserRegistrationDto
                .builder()
                .email(USER2_EMAIL)
                .userName(TEST_USER_NAME_2)
                .password(USER2_PASSWORD)
                .build();
        userRepository.save(mapperService.toEntity(dto2));
        String dtoJson2 = mapper.writeValueAsString(
                LoginDto.builder()
                        .email(USER2_EMAIL)
                        .password(USER2_PASSWORD)
                        .build());
        MvcResult result2 = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson2))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse2 = result2.getResponse().getContentAsString();
        TokenResponseDto responseDto2 = mapper.readValue(jsonResponse2, TokenResponseDto.class);
        accessToken2 = responseDto2.getAccessToken();
        currentUserId2 = responseDto2.getUserId();
    }

    private List<FileResponseDto> uploadAndMoveSomeFileForTest(int countOfFiles, String token, boolean isNotTemp) throws Exception {
        Random random = new Random();
        List<FileResponseDto> dtoList = new ArrayList<>();
        for (int i = 0; i < countOfFiles; i++) {
            FileResponseDto dto = uploadTextFile("testfile" + i + ".txt",
                    i + "This is the content of the test file" + i,
                    "Test file description" + i,
                    "Test folder name" + i,
                    "Test folder description" + i,
                    "",
                    token,
                    random.nextBoolean());
            if (isNotTemp) {
                FileResponseDto movedDto = moveTempFileInBucket(dto.getFileId(), token);
                dtoList.add(movedDto);
            } else {
                dtoList.add(dto);
            }
        }
        return dtoList;
    }

    private FileResponseDto uploadTextFile(
            String fileName,
            String fileContent,
            String fileDescription,
            String folderName,
            String folderDescription,
            String folderPath,
            String token,
            boolean isFolderProtected) throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                fileContent.getBytes()
        );
        MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                        .file(mockFile)
                        .param("fileDescription", fileDescription)
                        .param("folderName", folderName)
                        .param("folderDescription", folderDescription)
                        .param("folderPath", folderPath)
                        .param("isFolderProtected", String.valueOf(isFolderProtected))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
        return responseDto;
    }

    private FileResponseDto moveTempFileInBucket(UUID fileId, String token) throws Exception {
        String jsonDto = mapper.writeValueAsString(
                FileDto.builder()
                        .fileId(fileId)
                        .build());
        MvcResult result = mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
        return responseDto;
    }

    @Nested
    @DisplayName("Clean temporary files tests")
    class CleanTemporaryFilesTest {

        @Test
        public void clean_temporary_files_positive_test() throws Exception {

            List<FileResponseDto> uploadUser1ToTempBefore =
                    uploadAndMoveSomeFileForTest(5, accessToken1, false);
            List<FileResponseDto> uploadUser2ToTempBefore =
                    uploadAndMoveSomeFileForTest(3, accessToken2, false);

            List<FileResponseDto> uploadUser1ToStorageBefore =
                    uploadAndMoveSomeFileForTest(4, accessToken1, true);
            List<FileResponseDto> uploadUser2ToStorageBefore =
                    uploadAndMoveSomeFileForTest(6, accessToken2, true);

            Thread.sleep(10000);

            List<FileResponseDto> uploadUser1ToTempAfter =
                    uploadAndMoveSomeFileForTest(4, accessToken1, false);
            List<FileResponseDto> uploadUser2ToTempAfter =
                    uploadAndMoveSomeFileForTest(2, accessToken2, false);
            List<FileResponseDto> uploadUser1ToStorageAfter =
                    uploadAndMoveSomeFileForTest(3, accessToken1, true);
            List<FileResponseDto> uploadUser2ToStorageAfter =
                    uploadAndMoveSomeFileForTest(5, accessToken2, true);

            temporaryFilesCleaner.cleanTemporaryFiles();

            List<FileResponseDto> allCurrentFile = new ArrayList<>();
            allCurrentFile.addAll(uploadUser1ToStorageBefore);
            allCurrentFile.addAll(uploadUser2ToStorageBefore);
            allCurrentFile.addAll(uploadUser1ToTempAfter);
            allCurrentFile.addAll(uploadUser2ToTempAfter);
            allCurrentFile.addAll(uploadUser1ToStorageAfter);
            allCurrentFile.addAll(uploadUser2ToStorageAfter);

            List<UUID> fileInfoList = allCurrentFile.stream()
                    .map(FileResponseDto::getFileId).toList();
            List<FileInfo> fileInfos = fileService.findAllFilesInfosByFilesId(fileInfoList);
            assertEquals(fileInfos.size(), allCurrentFile.size());
            fileInfos.forEach(f ->
                    assertTrue(dataStorageService.isObjectExist(f.getPath(), f.getBucketName())
                    ));

            List<FileResponseDto> deletedFiles = new ArrayList<>();
            deletedFiles.addAll(uploadUser1ToTempBefore);
            deletedFiles.addAll(uploadUser2ToTempBefore);

            List<UUID> deletedFileInfoList = deletedFiles.stream()
                    .map(FileResponseDto::getFileId).toList();
            List<FileInfo> deletedFileInfos = fileService.findAllFilesInfosByFilesId(deletedFileInfoList);
            assertEquals(deletedFileInfos.size(), 0);
            deletedFileInfos.forEach(f ->
                    assertFalse(dataStorageService.isObjectExist(f.getPath(), f.getBucketName())
                    ));
        }

        @Test
        public void clean_temporary_files_positive_test_when_old_file_absent() throws Exception {

            List<FileResponseDto> uploadUser1ToStorageBefore =
                    uploadAndMoveSomeFileForTest(4, accessToken1, true);
            List<FileResponseDto> uploadUser2ToStorageBefore =
                    uploadAndMoveSomeFileForTest(6, accessToken2, true);

            List<FileResponseDto> uploadUser1ToStorageAfter =
                    uploadAndMoveSomeFileForTest(3, accessToken1, true);
            List<FileResponseDto> uploadUser2ToStorageAfter =
                    uploadAndMoveSomeFileForTest(5, accessToken2, true);

            temporaryFilesCleaner.cleanTemporaryFiles();

            List<FileResponseDto> allCurrentFile = new ArrayList<>();
            allCurrentFile.addAll(uploadUser1ToStorageBefore);
            allCurrentFile.addAll(uploadUser2ToStorageBefore);
            allCurrentFile.addAll(uploadUser1ToStorageAfter);
            allCurrentFile.addAll(uploadUser2ToStorageAfter);

            List<UUID> fileInfoList = allCurrentFile.stream()
                    .map(FileResponseDto::getFileId).toList();
            List<FileInfo> fileInfos = fileService.findAllFilesInfosByFilesId(fileInfoList);
            assertEquals(fileInfos.size(), allCurrentFile.size());
            fileInfos.forEach(f ->
                    assertTrue(dataStorageService.isObjectExist(f.getPath(), f.getBucketName())
                    ));
        }
    }
}