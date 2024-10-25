package compress.data_keeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.contstants.Role;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileInfoService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.UserMapperService;
import compress.data_keeper.services.utilities.FileUtilities;
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

import java.util.*;
import java.util.stream.Collectors;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.services.interfaces.FileService.ORIGINAL_FILE_KEY;
import static compress.data_keeper.services.utilities.FileUtilities.getFolderPathByFilePath;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
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

    private String adminAccessToken;
    private Long currentAdminId;

    private List<String> uploadedObjectPath = new ArrayList<>();

    private static final String USER1_EMAIL = "Test1" + "@example.com";
    private static final String USER1_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_1 = "TestName1";

    private static final String USER2_EMAIL = "Test2" + "@example.com";
    private static final String USER2_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_2 = "TestName2";

    private static final String ADMIN_EMAIL = "Admin" + "@example.com";
    private static final String ADMIN_PASSWORD = "Querty123!";
    private static final String TEST_ADMIN_NAME = "Admin TestName";

    private static final String FILE_TEMP_LOAD_PATH = "/v1/file/temp";
    private static final String SAVE_TEMP_FILE_PATH = "/v1/file/save";
    private static final String GET_ALL_FILES_PATH = "/v1/file/all";
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

    @Transactional
    protected void loginAdmin() throws Exception {
        UserRegistrationDto dto = UserRegistrationDto
                .builder()
                .email(ADMIN_EMAIL)
                .userName(TEST_ADMIN_NAME)
                .password(ADMIN_PASSWORD)
                .build();

        User admin = userRepository.save(mapperService.toEntity(dto));
        admin.setRole(Role.ROLE_ADMIN);

        String dtoJson = mapper.writeValueAsString(
                LoginDto.builder()
                        .email(ADMIN_EMAIL)
                        .password(ADMIN_PASSWORD)
                        .build());
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        TokenResponseDto responseDto = mapper.readValue(jsonResponse, TokenResponseDto.class);
        adminAccessToken = responseDto.getAccessToken();
        currentAdminId = responseDto.getUserId();
    }

    private void checkResponse(FileResponseDto responseDto, String bucketName) {
        checkOriginalFilePath(responseDto, bucketName);
        checkImageFilePathIfPathExists(responseDto);
    }

    private void checkOriginalFilePath(FileResponseDto responseDto, String bucketName) {
        String originalFileLink = responseDto.getLinksToFiles().get(ORIGINAL_FILE_KEY);
        assertNotNull(originalFileLink);
        assertInstanceOf(String.class, originalFileLink);

        UUID originalFileId = responseDto.getFileId();
        assertNotNull(originalFileId);

        String originalFilePath = fileInfoService.findOriginalFileInfoById(originalFileId).getPath();
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

        UUID originalFileId = responseDto.getFileId();
        String originalFilePath = fileInfoService.findOriginalFileInfoById(originalFileId).getPath();

        String folderPath = getFolderPathByFilePath(originalFilePath);
        Folder folder = folderService.getFolderByFolderPath(folderPath);
        assertNotNull(folder);
        assertEquals(folder.getOwner().getId(), currentUserId1);
        assertEquals(folder.getBucketName(), bucketName);

        List<FileInfo> filesInfos = fileInfoService.getFileInfoByFolderId(folder.getId());
        assertNotNull(filesInfos);
        filesInfos.forEach(f -> {
            assertNotNull(f);
            assertEquals(f.getBucketName(), bucketName);
        });

    }

    private UUID uploadTextFile(
            String fileName,
            String fileContent,
            String fileDescription,
            String folderName,
            String folderDescription,
            String folderPath,
            String token) throws Exception {
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
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
        return responseDto.getFileId();
    }

    private UUID uploadTextFile(
            String fileName,
            String fileContent,
            String fileDescription,
            String folderName,
            String folderDescription,
            String folderPath) throws Exception {
        return uploadTextFile(fileName,
                fileContent,
                fileDescription,
                folderName,
                folderDescription,
                folderPath,
                accessToken1);
    }

    private void moveTempFileInBucket(UUID fileId) throws Exception {
        String jsonDto = mapper.writeValueAsString(
                FileDto.builder()
                        .fileId(fileId)
                        .build());

        mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("POST /v1/file/temp")
    class FileTempUploadTests {

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
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
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
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
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
            UUID originalFileId = responseDto.getFileId();
            String originalFilePath = fileInfoService.findOriginalFileInfoById(originalFileId).getPath();
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
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
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
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
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
        public void create_file_temp_status_404_when_folder_path_is_not_found() throws Exception {

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
    class SaveTempFileInBucketTests {

        private UUID originalFileId;

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
            originalFileId = responseDto.getFileId();
        }

        @Test
        public void save_temp_files_with_status_200() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
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
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkResponse(responseDto, bucketName);
            checkFileAndFolderInfoDBData(responseDto, bucketName);
        }

        @Test
        public void save_temp_files_with_status_200_when_user_is_admin() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
                            .build());
            loginAdmin();

            MvcResult result = mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.linksIsValidForMs", isA(Number.class)))
                    .andExpect(jsonPath("$.linksIsValidForMs", greaterThan(0)))
                    .andExpect(jsonPath("$.linksToFiles").isMap())
                    .andExpect(jsonPath("$.fileId",
                            matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            checkResponse(responseDto, bucketName);
            checkFileAndFolderInfoDBData(responseDto, bucketName);
        }

        @Test
        public void save_temp_files_with_status_404_when_original_file_is_not_found() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(UUID.randomUUID())
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
        public void save_temp_files_with_status_404_when_file_id_is_not_original_file_id() throws Exception {

            Folder folder = fileInfoService.findOriginalFileInfoById(originalFileId).getFolder();
            List<FileInfo> fileInfos = fileInfoService.getFileInfoByFolderId(folder.getId());
            UUID someFileId = fileInfos.stream()
                    .filter(f -> !f.getIsOriginalFile())
                    .findFirst()
                    .map(FileInfo::getId)
                    .orElseThrow(Exception::new);

            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(someFileId)
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
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
                            .build());
            loginUser2();

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
        public void save_temp_files_with_status_404_when_file_in_folder_not_found() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
                            .build());

            String originalFilePath = fileInfoService.findOriginalFileInfoById(originalFileId).getPath();
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
        public void save_temp_files_with_status_400_when_file_is_not_in_temp_bucket() throws Exception {

            FileInfoDto fileInfoDto = new FileInfoDto();
            fileInfoDto.setBucketName("testbucketname");

            FileInfo fi = fileInfoService.updateFileInfo(originalFileId, fileInfoDto);
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
                            .build());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void save_temp_files_with_status_401_when_user_is_unauthorized() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
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

        @Test
        public void save_temp_files_with_status_400_when_id_is_incorrect() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .build());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    @Nested
    @DisplayName("GET /v1/file/all")
    class GetAllFilesTests {

        @BeforeEach
        void setUp() throws Exception {
            loginAdmin();
        }

        @Test
        public void get_all_files_with_status_200() throws Exception {
            Random random = new Random();
            int countOfFiles = random.nextInt(12) + 20;
            for (int i = 0; i < countOfFiles; i++) {
                UUID fileId = uploadTextFile("testfile" + i + ".txt",
                        i + "This is the content of the test file" + i,
                        "Test file description" + i,
                        "Test folder name" + i,
                        "Test folder description" + i,
                        "");
                if (random.nextBoolean()) {
                    moveTempFileInBucket(fileId);
                }
            }

            MvcResult result = mockMvc.perform(get(GET_ALL_FILES_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.files").isArray())
                    .andExpect(jsonPath("$.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageSize", is(10)))
                    .andExpect(jsonPath("$.totalPages", is((countOfFiles + 10 - 1) / 10)))
                    .andExpect(jsonPath("$.totalElements", is(countOfFiles)))
                    .andExpect(jsonPath("$.isFirstPage", is(true)))
                    .andExpect(jsonPath("$.isLastPage", is(false)))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDtoWithPagination responseDto = mapper.readValue(jsonResponse, FileResponseDtoWithPagination.class);

        }
    }
}
