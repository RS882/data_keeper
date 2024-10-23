package compress.data_keeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.UserMapperService;
import compress.data_keeper.services.utilities.FileUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.services.interfaces.FileService.ORIGINAL_FILE_KEY;
import static compress.data_keeper.services.utilities.FileUtilities.getFolderPathByFilePath;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("File controller integration tests: ")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
@Transactional
@Rollback
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapperService mapperService;

    @Autowired
    private DataStorageService dataStorageService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileInfoService fileInfoService;

    @Value("${bucket.name}")
    private String bucketName;

    @Value("${bucket.temp}")
    private String tempBucketName;

    private ObjectMapper mapper = new ObjectMapper();

    private String accessToken1;
    private Long currentUserId1;

    private String accessToken2;
    private Long currentUserId2;

    private List<String> uploadedObjectPath = new ArrayList<>();

    private static final String USER1_EMAIL = "Test1" + "@example.com";
    private static final String USER1_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_1 = "TestName1";

    private static final String USER2_EMAIL = "Test2" + "@example.com";
    private static final String USER2_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_2 = "TestName2";

    private static final String FILE_TEMP_LOAD_PATH = "/v1/file/temp";
    private static final String SAVE_TEMP_FILE_PATH = "/v1/file/save";
    private final String LOGIN_URL = "/v1/auth/login";

    @BeforeAll
    void createTestBucket() throws Exception {
        dataStorageService.checkAndCreateBucket(bucketName, false);
        dataStorageService.checkAndCreateBucket(tempBucketName, false);
    }

    @AfterAll
    public void cleanUpMinio() {
        dataStorageService.clearAndDeleteBucket(bucketName);
        dataStorageService.clearAndDeleteBucket(tempBucketName);
    }

    @BeforeEach
    void setUp() throws Exception {
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

    private void checkResponse(FileResponseDto responseDto, String bucketName) {
        checkOriginalFilePath(responseDto, bucketName);
        checkImageFilePathIfPathExists(responseDto);
    }

    private void checkOriginalFilePath(FileResponseDto responseDto, String bucketName) {
        String originalFileLink = responseDto.getLinksToFiles().get(ORIGINAL_FILE_KEY);
        assertNotNull(originalFileLink);
        assertInstanceOf(String.class, originalFileLink);

        String originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
        assertNotNull(originalFilePath);
        assertInstanceOf(String.class, originalFilePath);
        assertTrue(dataStorageService.isObjectExist(originalFilePath, bucketName));
    }

    private void checkImageFilePathIfPathExists(FileResponseDto responseDto) {
        Set<String> sizes = IMAGE_SIZES.stream()
                .map(FileUtilities::getNameFromSizes)
                .collect(Collectors.toSet());

        sizes.forEach(s -> {
            String linkValue = responseDto.getLinksToFiles().get(s);
            assertNotNull(linkValue);
            assertInstanceOf(String.class, linkValue);
        });
    }

    private void checkImageFilePathIfPathNotExists(FileResponseDto responseDto) {
        Set<String> sizes = IMAGE_SIZES.stream()
                .map(FileUtilities::getNameFromSizes)
                .collect(Collectors.toSet());

        sizes.forEach(s -> {
            String linkValue = responseDto.getLinksToFiles().get(s);
            assertNull(linkValue);
        });
    }

    private void checkFileAndFolderInfoDBData(FileResponseDto responseDto, String bucketName) {

        String filePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
        String folderPath =getFolderPathByFilePath(filePath);
        Folder folder = folderService.getFolderByFolderPath(folderPath);
        assertNotNull(folder);
        assertEquals(folder.getOwner().getId(),currentUserId1);
        assertEquals(folder.getBucketName(),bucketName);

        List<FileInfo> filesInfos = fileInfoService.getFileInfoByFolderId(folder.getId());
        assertNotNull(filesInfos);
        filesInfos.forEach(f->{
            assertNotNull(f);
            assertEquals(f.getBucketName(),bucketName);
        });

    }

    @Nested
    @DisplayName("POST /v1/file/temp")
    class FileTempUploadTest {

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_new_dir() throws Exception {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );
            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.paths").isMap())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);
        }

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_new_dir_without_file_param() throws Exception {

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.paths").isMap())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);
        }

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_old_dir() throws Exception {

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );

            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
            String originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
            String pathFolder = originalFilePath.substring(0, originalFilePath.lastIndexOf("/")).trim();

            MockMultipartFile mockFile2 = new MockMultipartFile(
                    "file",
                    "testfile2.txt",
                    "text/plain",
                    "This is the content of the test file2".getBytes()
            );
            String fileDescription2 = "Test file description";
            String folderName2 = "Test folder name";
            String folderDescription2 = "Test folder description";
            String folderPath2 = pathFolder;

            MvcResult result2 = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile2)
                            .param("fileDescription", fileDescription2)
                            .param("folderName", folderName2)
                            .param("folderDescription", folderDescription2)
                            .param("folderPath", folderPath2)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.paths").isMap())
                    .andReturn();

            String jsonResponse2 = result2.getResponse().getContentAsString();

            FileResponseDto responseDto2 = mapper.readValue(jsonResponse2, FileResponseDto.class);

            checkResponse(responseDto2, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto2, tempBucketName);
        }

        @Test
        public void create_file_temp_status_200_for_new_some_file_in_new_dir() throws Exception {

            byte[] randomBytes = new byte[256];
            new Random().nextBytes(randomBytes);

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.bin",
                    "test/test",
                    randomBytes
            );
            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.paths").isMap())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkOriginalFilePath(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);

            checkImageFilePathIfPathNotExists(responseDto);

        }

        @Test
        public void create_file_temp_status_400_when_file_is_empty() throws Exception {

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    new byte[0]
            );
            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void create_file_temp_status_404_when_folder_path_is_wrong() throws Exception {

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );

            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = UUID.randomUUID().toString();

            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void create_file_temp_status_401_when_user_is_unauthorized() throws Exception {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );
            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "test"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/file/save")
    class SaveTempFileInBucket {

        private String originalFilePath;

        @BeforeEach
        void setUp() throws Exception {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file",
                    "testfile.txt",
                    "text/plain",
                    "This is the content of the test file".getBytes()
            );
            String fileDescription = "Test file description";
            String folderName = "Test folder name";
            String folderDescription = "Test folder description";
            String folderPath = "";

            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", fileDescription)
                            .param("folderName", folderName)
                            .param("folderDescription", folderDescription)
                            .param("folderPath", folderPath)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
            originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
        }

        @Test
        public void save_temp_files_with_status_200() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .filePath(originalFilePath)
                            .build());

            MvcResult result = mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.paths").isMap())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkResponse(responseDto, bucketName);
            checkFileAndFolderInfoDBData(responseDto, bucketName);
        }

        @Test
        public void save_temp_files_with_status_404_when_file_path_is_wrong() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .filePath("Test/path")
                            .build());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void save_temp_files_with_status_403_when_user_dont_have_right() throws Exception {
            loginUser2();
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .filePath(originalFilePath)
                            .build());
            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void save_temp_files_with_status_404_when_folder_is_empty() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .filePath(originalFilePath)
                            .build());

            String folderPath = getFolderPathByFilePath(originalFilePath);
            Folder folder = folderService.getFolderByFolderPath(folderPath);
            fileInfoService.deleteAllFileInfosByFolderId(folder.getId());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void save_temp_files_with_status_401_when_user_is_unauthorized() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .filePath(originalFilePath)
                            .build());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "test"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @ParameterizedTest(name = "Тест {index}: save_temp_files_with_status_400_when_path_is_incorrect [{arguments}]")
        @MethodSource("incorrectFilePaths")
        public void save_temp_files_with_status_400_when_path_is_incorrect(FileDto dto) throws Exception {
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }

        private static Stream<Arguments> incorrectFilePaths() {
            return Stream.of(Arguments.of(
                            FileDto.builder()
                                    .build()),
                    Arguments.of(
                            FileDto.builder()
                                    .filePath("")
                                    .build()),
                    Arguments.of(
                            FileDto.builder()
                                    .filePath("       ")
                                    .build())
            );
        }
    }
}