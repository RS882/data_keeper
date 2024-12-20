package compress.data_keeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import compress.data_keeper.domain.dto.file_info.FileInfoDto;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.files.FileResponseDtoWithPagination;
import compress.data_keeper.domain.dto.files.FileUpdateDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.exception_handler.not_found.exceptions.FileInfoNotFoundException;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.contstants.Role;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.mapping.UserMapperService;
import compress.data_keeper.services.utilities.FileUtilities;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.services.FolderServiceImpl.DEFAULT_FOLDER_PREFIX_NAME;
import static compress.data_keeper.services.interfaces.FileService.ORIGINAL_FILE_KEY;
import static compress.data_keeper.services.utilities.FileUtilities.getFolderPathByFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
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
    private FileService fileService;

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
    private static final String GET_ALL_USERS_FILES_PATH = "/v1/file/all/user/{id}";
    private static final String GET_FILE_BY_ID_PATH = "/v1/file/{id}";
    private static final String DELETE_FILE_BY_ID_PATH = "/v1/file/{id}";
    private static final String PUT_FILE_INFO_PATH = "/v1/file/update/info";

    private static final String LOGIN_URL = "/v1/auth/login";

    private static final String TEST_TXT_FILE_NAME = "testfile.txt";
    private static final String TEST_FILE_DESCRIPTION = "Test file description";
    private static final String TEST_FOLDER_NAME = "Test folder name";
    private static final String TEST_FOLDER_DESCRIPTION = "Test folder description";
    private static final String FOLDER_PATH = "";
    private static final String TEXT_FILE_TYPE = "text/plain";
    private static final String DOCX_FILE_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";


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
        mapper.registerModule(new JavaTimeModule());
        loginUser1();
    }

    private void loginUser1() throws Exception {
        TokenResponseDto responseDto = loginUser(USER1_EMAIL, TEST_USER_NAME_1, USER1_PASSWORD);
        accessToken1 = responseDto.getAccessToken();
        currentUserId1 = responseDto.getUserId();
    }

    private void loginUser2() throws Exception {
        TokenResponseDto responseDto = loginUser(USER2_EMAIL, TEST_USER_NAME_2, USER2_PASSWORD);
        accessToken2 = responseDto.getAccessToken();
        currentUserId2 = responseDto.getUserId();
    }

    private TokenResponseDto loginUser(String email, String name, String password) throws Exception {
        UserRegistrationDto dto = UserRegistrationDto
                .builder()
                .email(email)
                .userName(name)
                .password(password)
                .build();
        userRepository.save(mapperService.toEntity(dto));
        String dtoJson = mapper.writeValueAsString(
                LoginDto.builder()
                        .email(email)
                        .password(password)
                        .build());
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk())
                .andReturn();
        String jsonResponse = result.getResponse().getContentAsString();
        return mapper.readValue(jsonResponse, TokenResponseDto.class);
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

        String originalFilePath = fileService.findOriginalFileInfoById(responseDto.getFileId()).getPath();
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

    private void checkFileAndFolderInfoDBData(FileResponseDto responseDto, String bucketName,
                                              boolean isTemp, boolean isFolderProtected) {
        Folder folder = getFolderByFileResponseDto(responseDto);
        checkFileAndFolderInfoDBDataWithoutFolderIsTemp(folder, bucketName, responseDto.getFileId());
        assertEquals(folder.isTemp(), isTemp);
        assertEquals(folder.isProtected(), isFolderProtected);
    }

    private void checkFileAndFolderInfoDBData(FileResponseDto responseDto, String bucketName, boolean isTemp) {
        Folder folder = getFolderByFileResponseDto(responseDto);
        checkFileAndFolderInfoDBDataWithoutFolderIsTemp(folder, bucketName, responseDto.getFileId());
        assertEquals(folder.isTemp(), isTemp);
    }

    private void checkFileAndFolderInfoDBData(FileResponseDto responseDto, String bucketName) {
        Folder folder = getFolderByFileResponseDto(responseDto);
        checkFileAndFolderInfoDBDataWithoutFolderIsTemp(folder, bucketName, responseDto.getFileId());
    }

    private Folder getFolderByFileResponseDto(FileResponseDto responseDto) {

        FileInfo fileInfo = fileService.findOriginalFileInfoById(responseDto.getFileId());
        return fileInfo.getFolder();
    }

    private void checkFileAndFolderInfoDBDataWithoutFolderIsTemp(Folder folder, String bucketName, UUID originalFileId) {
        assertNotNull(folder);
        assertEquals(folder.getOwner().getId(), currentUserId1);
        assertEquals(folder.getBucketName(), bucketName);

        List<FileInfo> filesInfos = fileService.findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), originalFileId);
        assertNotNull(filesInfos);
        filesInfos.forEach(f -> {
            assertNotNull(f);
            assertEquals(f.getBucketName(), bucketName);
        });
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
        MockMultipartFile mockFile = createMockTxtFile(fileName, fileContent.getBytes());
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

    private List<FileResponseDto> uploadAndMoveSomeFileForTest() throws Exception {
        Random random = new Random();
        int countOfFiles = random.nextInt(12) + 20;
        return uploadAndMoveSomeFileForTest(countOfFiles, accessToken1);
    }

    private List<FileResponseDto> uploadAndMoveSomeFileForTest(int countOfFiles, String token) throws Exception {
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
            if (random.nextBoolean()) {
                FileResponseDto movedDto = moveTempFileInBucket(dto.getFileId(), token);
                dtoList.add(movedDto);
            } else {
                dtoList.add(dto);
            }
        }
        return dtoList;
    }

    private void verifyFileResponseDto(FileResponseDto dto) {
        verifyFileResponseDto(dto,
                TEST_TXT_FILE_NAME,
                TEST_FILE_DESCRIPTION,
                TEST_FOLDER_NAME,
                TEST_FOLDER_DESCRIPTION,
                TEXT_FILE_TYPE);
    }

    private void verifyFileResponseDto(FileResponseDto dto,
                                       String fileName,
                                       String fileDescription,
                                       String folderName,
                                       String folderDescription) {
        verifyFileResponseDto(dto, fileName, fileDescription, folderName, folderDescription, TEXT_FILE_TYPE);
    }

    private void verifyFileResponseDto(FileResponseDto dto,
                                       String fileName,
                                       String fileDescription,
                                       String folderName,
                                       String folderDescription,
                                       String fileType) {
        assertNotNull(dto);
        assertNotNull(dto.getLinksIsValidUntil());
        assertThat(dto.getLinksIsValidUntil()).isInstanceOf(ZonedDateTime.class);
        assertThat(dto.getLinksToFiles()).isInstanceOf(Map.class);
        assertNotNull(dto.getFileId());
        assertThat(dto.getFileId()).isInstanceOf(UUID.class);
        assertEquals(fileName, dto.getFileName());
        assertEquals(fileDescription, dto.getFileDescription());
        assertEquals(fileType, dto.getFileType());
        if (folderName == null) {
            assertThat(dto.getFolderName()).startsWith(DEFAULT_FOLDER_PREFIX_NAME);
        } else {
            assertEquals(folderName, dto.getFolderName());
        }
        assertEquals(folderDescription, dto.getFolderDescription());
    }

    private MockMultipartFile createMockDocxFile(String fileName, String content) throws IOException {

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = doc.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(content);

            doc.write(out);
            return new MockMultipartFile(
                    "file",
                    fileName,
                    DOCX_FILE_TYPE,
                    out.toByteArray());
        }
    }

    private MockMultipartFile createMockTxtFile(String fileName, byte[] content) {
        return new MockMultipartFile(
                "file",
                fileName,
                TEXT_FILE_TYPE,
                content);
    }

    private MockMultipartFile createMockTxtFile(byte[] content) {
        return createMockTxtFile(TEST_TXT_FILE_NAME, content);
    }

    private MockMultipartFile createMockTxtFile(String fileName) {
        return createMockTxtFile(fileName, "This is the content of the test file".getBytes());
    }

    private MockMultipartFile createMockTxtFile() {
        return createMockTxtFile(TEST_TXT_FILE_NAME);
    }

    @Nested
    @DisplayName("POST /v1/file/temp")
    class FileTempUploadTests {

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_new_dir() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true, false);
        }

        @Test
        public void create_file_temp_status_200_for_new_docx_file_in_new_dir() throws Exception {
            String docxFileName = "test.docx";
            MockMultipartFile mockFile = createMockDocxFile(docxFileName, "This is the content of the test docx file");
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto, docxFileName, TEST_FILE_DESCRIPTION, TEST_FOLDER_NAME, TEST_FOLDER_DESCRIPTION, DOCX_FILE_TYPE);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true, false);
        }

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_new_dir_when_folder_is_protected() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .param("isFolderProtected", "true")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true, true);
        }

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_new_dir_without_file_param() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
            verifyFileResponseDto(responseDto, TEST_TXT_FILE_NAME, null, null, null);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true);
        }

        @Test
        public void create_file_temp_status_200_for_new_txt_file_in_old_dir() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
            UUID originalFileId = responseDto.getFileId();
            String originalFilePath = fileService.findOriginalFileInfoById(originalFileId).getPath();
            String pathFolder = originalFilePath.substring(0, originalFilePath.lastIndexOf("/")).trim();

            String fileName2 = "testfile2.txt";
            String fileDescription2 = "2Test file description2";
            String folderPath2 = pathFolder;
            MockMultipartFile mockFile2 = createMockTxtFile(fileName2);
            MvcResult result2 = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile2)
                            .param("fileDescription", fileDescription2)
                            .param("folderPath", folderPath2)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String jsonResponse2 = result2.getResponse().getContentAsString();
            FileResponseDto responseDto2 = mapper.readValue(jsonResponse2, FileResponseDto.class);

            verifyFileResponseDto(responseDto2, fileName2, fileDescription2, TEST_FOLDER_NAME, TEST_FOLDER_DESCRIPTION);
            checkResponse(responseDto2, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto2, tempBucketName, true);
        }

        @Test
        public void create_file_temp_status_200_for_new_some_file_in_new_dir() throws Exception {
            byte[] randomBytes = new byte[256];
            new Random().nextBytes(randomBytes);
            MockMultipartFile mockFile = createMockTxtFile(randomBytes);
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();

            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkOriginalFilePath(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true);

            checkImageFilePathIfPathNotExists(responseDto);
        }

        @Test
        public void create_file_temp_status_400_when_file_is_empty() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile(new byte[0]);
            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void create_file_temp_status_401_when_user_isnt_authorized() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "testtext"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
            ;
        }

        @Test
        public void create_file_temp_status_404_when_folder_path_is_not_found() throws Exception {
            String folderPath = UUID.randomUUID().toString();
            MockMultipartFile mockFile = createMockTxtFile();
            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
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
            MockMultipartFile mockFile = createMockTxtFile();
            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
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
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
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
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, bucketName);
            checkFileAndFolderInfoDBData(responseDto, bucketName, false);
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
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, bucketName);
            checkFileAndFolderInfoDBData(responseDto, bucketName, false);
        }

        @Test
        public void save_temp_files_with_status_401_when_user_isnt_authorized() throws Exception {
            String jsonDto = mapper.writeValueAsString(
                    FileDto.builder()
                            .fileId(originalFileId)
                            .build());

            mockMvc.perform(patch(SAVE_TEMP_FILE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "testtext"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
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

            Folder folder = fileService.findOriginalFileInfoById(originalFileId).getFolder();
            List<FileInfo> fileInfos = fileService.findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), originalFileId);
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

            String originalFilePath = fileService.findOriginalFileInfoById(originalFileId).getPath();
            String folderPath = getFolderPathByFilePath(originalFilePath);
            Folder folder = folderService.getFolderByFolderPath(folderPath);
            fileService.deleteAllFileInfosByFolderId(folder.getId());

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

            fileService.updateFileInfo(originalFileId, fileInfoDto);
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
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
            List<FileResponseDto> uploadedFiles = uploadAndMoveSomeFileForTest();
            int countOfFiles = uploadedFiles.size();

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
            responseDto.getFiles().forEach(
                    f -> {
                        FileInfo fileInfo = fileService.findOriginalFileInfoById(f.getFileId());
                        String bucketName = fileInfo.getBucketName();
                        FileResponseDto fileResponseDto = uploadedFiles.stream()
                                .filter(uf -> uf.getFileId().equals(f.getFileId()))
                                .findFirst()
                                .orElse(null);
                        if (fileResponseDto == null) {
                            verifyFileResponseDto(f, fileInfo.getName(),
                                    null, null, null);
                        } else {
                            verifyFileResponseDto(f,
                                    fileResponseDto.getFileName(),
                                    fileResponseDto.getFileDescription(),
                                    fileResponseDto.getFolderName(),
                                    fileResponseDto.getFolderDescription());
                        }
                        checkResponse(f, bucketName);
                        checkFileAndFolderInfoDBData(f, bucketName);
                    }
            );
        }

        @Test
        public void get_all_files_with_status_200_for_txt_file_in_old_folder() throws Exception {
            MockMultipartFile mockFile = createMockTxtFile();
            MvcResult result = mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile)
                            .param("fileDescription", TEST_FILE_DESCRIPTION)
                            .param("folderName", TEST_FOLDER_NAME)
                            .param("folderDescription", TEST_FOLDER_DESCRIPTION)
                            .param("folderPath", FOLDER_PATH)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);
            UUID originalFileId = responseDto.getFileId();
            String originalFilePath = fileService.findOriginalFileInfoById(originalFileId).getPath();
            String pathFolder = originalFilePath.substring(0, originalFilePath.lastIndexOf("/")).trim();

            String fileName2 = "testfile2.txt";
            String fileDescription2 = "Test file description";
            String folderPath2 = pathFolder;
            MockMultipartFile mockFile2 = createMockTxtFile(fileName2);
            mockMvc.perform(multipart(FILE_TEMP_LOAD_PATH)
                            .file(mockFile2)
                            .param("fileDescription", fileDescription2)
                            .param("folderPath", folderPath2)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk());

            MvcResult result3 = mockMvc.perform(get(GET_ALL_FILES_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.files").isArray())
                    .andExpect(jsonPath("$.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageSize", is(10)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.isFirstPage", is(true)))
                    .andExpect(jsonPath("$.isLastPage", is(true)))
                    .andReturn();
            String jsonResponse3 = result3.getResponse().getContentAsString();
            FileResponseDtoWithPagination responseDto3 = mapper.readValue(jsonResponse3, FileResponseDtoWithPagination.class);
            responseDto3.getFiles().forEach(
                    f -> {
                        FileInfo fileInfo = fileService.findOriginalFileInfoById(f.getFileId());
                        String bucketName = fileInfo.getBucketName();
                        checkResponse(f, bucketName);
                        checkFileAndFolderInfoDBData(f, bucketName);
                    }
            );
        }

        @ParameterizedTest(name = "Test {index}: Get all files with status 400 when pagination is wrong[{arguments}]")
        @MethodSource("incorrectPaginationArgs")
        public void get_all_files_with_status_400_when_pagination_is_wrong(MultiValueMap<String, String> params) throws Exception {
            mockMvc.perform(get(GET_ALL_FILES_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .params(params)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        private static Stream<Arguments> incorrectPaginationArgs() {
            MultiValueMap<String, String> params1 = new LinkedMultiValueMap<>();
            params1.add("page", "-4");
            params1.add("size", "10");
            params1.add("sortBy", "createdAt");
            params1.add("isAsc", "true");

            MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
            params2.add("page", "0");
            params2.add("size", "-20");
            params2.add("sortBy", "updatedAt");
            params2.add("isAsc", "false");

            MultiValueMap<String, String> params3 = new LinkedMultiValueMap<>();
            params3.add("page", "-2");
            params3.add("size", "-10");
            params3.add("sortBy", "createdAt");
            params3.add("isAsc", "false");

            MultiValueMap<String, String> params4 = new LinkedMultiValueMap<>();
            params4.add("page", "3");
            params4.add("size", "0");
            params4.add("sortBy", "createdAt");
            params4.add("isAsc", "false");

            return Stream.of(
                    Arguments.of(params1),
                    Arguments.of(params2),
                    Arguments.of(params3),
                    Arguments.of(params4)
            );
        }

        @Test
        public void get_all_files_with_status_401_when_user_isnt_authorized() throws Exception {
            mockMvc.perform(get(GET_ALL_FILES_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "testtext"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void get_all_files_with_status_403_when_user_doesnt_have_admin_right() throws Exception {
            loginUser2();
            mockMvc.perform(get(GET_ALL_FILES_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /v1/file/all/user/{id}")
    class GetAllFilesByUserIdTests {

        @Test
        public void get_all_file_by_user_id_status_200() throws Exception {
            int countOfFilesUser1 = 8;
            int countOfFilesUser2 = 5;
            List<FileResponseDto> uploadFileUser1 = uploadAndMoveSomeFileForTest(countOfFilesUser1, accessToken1);
            int countOfFiles = uploadFileUser1.size();

            loginUser2();
            uploadAndMoveSomeFileForTest(countOfFilesUser2, accessToken2);

            MvcResult result = mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.files").isArray())
                    .andExpect(jsonPath("$.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageSize", is(10)))
                    .andExpect(jsonPath("$.totalPages", is((countOfFiles + 10 - 1) / 10)))
                    .andExpect(jsonPath("$.totalElements", is(countOfFiles)))
                    .andExpect(jsonPath("$.isFirstPage", is(true)))
                    .andExpect(jsonPath("$.isLastPage", is(true)))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDtoWithPagination responseDto = mapper.readValue(jsonResponse, FileResponseDtoWithPagination.class);
            responseDto.getFiles().forEach(
                    f -> {
                        FileInfo fileInfo = fileService.findOriginalFileInfoById(f.getFileId());
                        String bucketName = fileInfo.getBucketName();
                        FileResponseDto fileResponseDto = uploadFileUser1.stream()
                                .filter(uf -> uf.getFileId().equals(f.getFileId()))
                                .findFirst()
                                .orElse(null);
                        if (fileResponseDto == null) {
                            verifyFileResponseDto(f, fileInfo.getName(),
                                    null, null, null);
                        } else {
                            verifyFileResponseDto(f,
                                    fileResponseDto.getFileName(),
                                    fileResponseDto.getFileDescription(),
                                    fileResponseDto.getFolderName(),
                                    fileResponseDto.getFolderDescription());
                        }
                        checkResponse(f, bucketName);
                        checkFileAndFolderInfoDBData(f, bucketName);
                    }
            );
        }

        @Test
        public void get_all_file_by_user_id_status_200_when_dont_have_file_by_user_id() throws Exception {
            int countOfFilesUser2 = 5;
            loginUser2();
            uploadAndMoveSomeFileForTest(countOfFilesUser2, accessToken2);

            MvcResult result = mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.files").isArray())
                    .andExpect(jsonPath("$.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageSize", is(10)))
                    .andExpect(jsonPath("$.totalPages", is(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)))
                    .andExpect(jsonPath("$.isFirstPage", is(true)))
                    .andExpect(jsonPath("$.isLastPage", is(true)))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDtoWithPagination responseDto = mapper.readValue(jsonResponse, FileResponseDtoWithPagination.class);
            assertTrue(responseDto.getFiles().isEmpty());
        }

        @Test
        public void get_all_file_by_user_id_status_200_when_user_is_admin() throws Exception {
            int countOfFilesUser1 = 8;
            int countOfFilesUser2 = 5;
            List<FileResponseDto> uploadFileUser1 = uploadAndMoveSomeFileForTest(countOfFilesUser1, accessToken1);
            int countOfFiles = uploadFileUser1.size();

            loginUser2();
            uploadAndMoveSomeFileForTest(countOfFilesUser2, accessToken2);

            loginAdmin();

            MvcResult result = mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
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
                    .andExpect(jsonPath("$.isLastPage", is(true)))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDtoWithPagination responseDto = mapper.readValue(jsonResponse, FileResponseDtoWithPagination.class);
            responseDto.getFiles().forEach(
                    f -> {
                        FileInfo fileInfo = fileService.findOriginalFileInfoById(f.getFileId());
                        String bucketName = fileInfo.getBucketName();
                        FileResponseDto fileResponseDto = uploadFileUser1.stream()
                                .filter(uf -> uf.getFileId().equals(f.getFileId()))
                                .findFirst()
                                .orElse(null);
                        if (fileResponseDto == null) {
                            verifyFileResponseDto(f, fileInfo.getName(),
                                    null, null, null);
                        } else {
                            verifyFileResponseDto(f,
                                    fileResponseDto.getFileName(),
                                    fileResponseDto.getFileDescription(),
                                    fileResponseDto.getFolderName(),
                                    fileResponseDto.getFolderDescription());
                        }
                        checkResponse(f, bucketName);
                        checkFileAndFolderInfoDBData(f, bucketName);
                    }
            );
        }

        @ParameterizedTest(name = "Test {index}: Get all files by user id with status 400 when pagination is wrong[{arguments}]")
        @MethodSource("incorrectPaginationArgs")
        public void get_all_file_by_user_id_when_pagination_is_wrong(MultiValueMap<String, String> params) throws Exception {
            mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .params(params)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        private static Stream<Arguments> incorrectPaginationArgs() {
            MultiValueMap<String, String> params1 = new LinkedMultiValueMap<>();
            params1.add("page", "-4");
            params1.add("size", "10");
            params1.add("sortBy", "createdAt");
            params1.add("isAsc", "true");

            MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
            params2.add("page", "0");
            params2.add("size", "-20");
            params2.add("sortBy", "updatedAt");
            params2.add("isAsc", "false");

            MultiValueMap<String, String> params3 = new LinkedMultiValueMap<>();
            params3.add("page", "-2");
            params3.add("size", "-10");
            params3.add("sortBy", "createdAt");
            params3.add("isAsc", "false");

            MultiValueMap<String, String> params4 = new LinkedMultiValueMap<>();
            params4.add("page", "3");
            params4.add("size", "0");
            params4.add("sortBy", "createdAt");
            params4.add("isAsc", "false");

            return Stream.of(
                    Arguments.of(params1),
                    Arguments.of(params2),
                    Arguments.of(params3),
                    Arguments.of(params4)
            );
        }

        @ParameterizedTest(name = "Test {index}: Get all files by user id with status 400 when user id is wrong[{arguments}]")
        @ValueSource(longs = {0L, -34L})
        public void get_all_file_by_user_id_status_400_when_user_id_is_wrong(long userId) throws Exception {
            mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        public void get_all_file_by_user_id_status_403_user_id_dont_equals_owner_id() throws Exception {
            loginUser2();
            mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void get_all_file_by_user_id_status_401_when_user_isnt_authorized() throws Exception {
            mockMvc.perform(get(GET_ALL_USERS_FILES_PATH, currentUserId1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "testtext"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }
    }

    @Nested
    @DisplayName("GET /v1/file{id}")
    class GetFileByFileIdTests {

        private static FileResponseDto uploadFIleDto = new FileResponseDto();

        @BeforeEach
        void setUp() throws Exception {
            Random random = new Random();
            uploadFIleDto = uploadTextFile(TEST_TXT_FILE_NAME,
                    "This is the content of the test file",
                    TEST_FILE_DESCRIPTION,
                    TEST_FOLDER_NAME,
                    TEST_FOLDER_DESCRIPTION,
                    FOLDER_PATH,
                    accessToken1,
                    random.nextBoolean()
            );
        }

        @Test
        public void get_file_link_by_file_id_status_200() throws Exception {
            MvcResult result = mockMvc.perform(get(GET_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);
        }

        @Test
        public void get_file_link_by_file_id_status_200_when_user_is_admin() throws Exception {
            loginAdmin();
            MvcResult result = mockMvc.perform(get(GET_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            verifyFileResponseDto(responseDto);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);
        }

        @Test
        public void get_file_link_by_file_id_status_400_when_id_is_wnong() throws Exception {
            mockMvc.perform(get(GET_FILE_BY_ID_PATH, "123")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void get_file_link_by_file_id_status_401_user_is_not_authorized() throws Exception {
            mockMvc.perform(get(GET_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "kdkdkkkdkd"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void get_file_link_by_file_id_status_403_user_dont_have_right() throws Exception {
            loginUser2();
            mockMvc.perform(get(GET_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void get_file_link_by_file_id_status_404_when_file_not_found() throws Exception {
            mockMvc.perform(get(GET_FILE_BY_ID_PATH, UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }
    }

    @Nested
    @DisplayName("PUT /v1/file/update/info")
    class PutFileInfoTest {

        private static FileResponseDto uploadFIleDto = new FileResponseDto();

        @BeforeEach
        void setUp() throws Exception {
            Random random = new Random();
            uploadFIleDto = uploadTextFile(TEST_TXT_FILE_NAME,
                    "This is the content of the test file",
                    TEST_FILE_DESCRIPTION,
                    TEST_FOLDER_NAME,
                    TEST_FOLDER_DESCRIPTION,
                    FOLDER_PATH,
                    accessToken1,
                    random.nextBoolean()
            );
        }

        @ParameterizedTest(name = "Test {index}: Update file information with status 200 when update date is correct : {arguments}]")
        @MethodSource("correctFileUpdateDtoArgs")
        public void update_file_info_status_200(
                FileUpdateDto dto,
                String newFileName,
                String newFileDescription,
                String newFolderName,
                String newFolderDescription,
                boolean isFolderProtected) throws Exception {

            dto.setFileId(uploadFIleDto.getFileId());

            String jsonDto = mapper.writeValueAsString(dto);

            MvcResult result = mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            assertNotNull(responseDto);
            assertEquals(uploadFIleDto.getFileId(), responseDto.getFileId());

            verifyFileResponseDto(responseDto, newFileName, newFileDescription, newFolderName, newFolderDescription);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName, true, isFolderProtected);
        }

        private static Stream<Arguments> correctFileUpdateDtoArgs() {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";
            return Stream.of(
                    Arguments.of(FileUpdateDto.builder()
                                    .fileName(newFileName)
                                    .fileDescription(newFileDescription)
                                    .folderName(newFolderName)
                                    .folderDescription(newFolderDescription)
                                    .isFolderProtected(true)
                                    .build(),
                            newFileName,
                            newFileDescription,
                            newFolderName,
                            newFolderDescription,
                            true
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .build(),
                            TEST_TXT_FILE_NAME,
                            TEST_FILE_DESCRIPTION,
                            TEST_FOLDER_NAME,
                            TEST_FOLDER_DESCRIPTION,
                            false
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .isFolderProtected(true)
                                    .build(),
                            TEST_TXT_FILE_NAME,
                            TEST_FILE_DESCRIPTION,
                            TEST_FOLDER_NAME,
                            TEST_FOLDER_DESCRIPTION,
                            true
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .fileDescription(newFileDescription)
                                    .folderName(newFolderName)
                                    .folderDescription(newFolderDescription)
                                    .build(),
                            TEST_TXT_FILE_NAME,
                            newFileDescription,
                            newFolderName,
                            newFolderDescription,
                            false
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .fileName(newFileName)
                                    .folderName(newFolderName)
                                    .folderDescription(newFolderDescription)
                                    .build(),
                            newFileName,
                            TEST_FILE_DESCRIPTION,
                            newFolderName,
                            newFolderDescription,
                            false
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .fileName(newFileName)
                                    .fileDescription(newFileDescription)
                                    .folderDescription(newFolderDescription)
                                    .build(),
                            newFileName,
                            newFileDescription,
                            TEST_FOLDER_NAME,
                            newFolderDescription,
                            false
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .fileName(newFileName)
                                    .fileDescription(newFileDescription)
                                    .folderName(newFolderName)
                                    .build(),
                            newFileName,
                            newFileDescription,
                            newFolderName,
                            TEST_FOLDER_DESCRIPTION,
                            false
                    ),
                    Arguments.of(FileUpdateDto.builder()
                                    .fileDescription(newFileDescription)
                                    .folderName(newFolderName)
                                    .isFolderProtected(true)
                                    .build(),
                            TEST_TXT_FILE_NAME,
                            newFileDescription,
                            newFolderName,
                            TEST_FOLDER_DESCRIPTION,
                            true
                    )
            );
        }

        @Test
        public void update_file_info_status_200_when_user_is_admin() throws Exception {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";

            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileId(uploadFIleDto.getFileId())
                    .fileName(newFileName)
                    .fileDescription(newFileDescription)
                    .folderName(newFolderName)
                    .folderDescription(newFolderDescription)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            loginAdmin();

            MvcResult result = mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String jsonResponse = result.getResponse().getContentAsString();
            FileResponseDto responseDto = mapper.readValue(jsonResponse, FileResponseDto.class);

            assertNotNull(responseDto);
            assertEquals(uploadFIleDto.getFileId(), responseDto.getFileId());

            verifyFileResponseDto(responseDto, newFileName, newFileDescription, newFolderName, newFolderDescription);
            checkResponse(responseDto, tempBucketName);
            checkFileAndFolderInfoDBData(responseDto, tempBucketName);
        }

        @Test
        public void update_file_info_status_400_when_file_update_dto_is_null() throws Exception {
            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void update_file_info_status_400_when_file_id_is_null() throws Exception {
            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileName(TEST_TXT_FILE_NAME)
                    .fileDescription(TEST_FILE_DESCRIPTION)
                    .folderName(TEST_FOLDER_NAME)
                    .folderDescription(TEST_FOLDER_DESCRIPTION)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @ParameterizedTest(name = "Test {index}: Update file information with status 400 when update date is wrong : {arguments}]")
        @MethodSource("incorrectFileUpdateDtoArgs")
        public void update_file_info_status_400_when_values_dto_fields_is_wrong(FileUpdateDto dto) throws Exception {
            dto.setFileId(uploadFIleDto.getFileId());
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        private static Stream<Arguments> incorrectFileUpdateDtoArgs() {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";

            return Stream.of(
                    Arguments.of(FileUpdateDto.builder()
                            .fileName(newFileName)
                            .fileDescription("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcH")
                            .folderName(newFolderName)
                            .folderDescription(newFolderDescription)
                            .build()
                    ),
                    Arguments.of(FileUpdateDto.builder()
                            .fileName("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZr.uisi")
                            .fileDescription(newFileDescription)
                            .folderName(newFolderName)
                            .folderDescription(newFolderDescription)
                            .build()
                    ),
                    Arguments.of(FileUpdateDto.builder()
                            .fileName(newFileName)
                            .fileDescription(newFileDescription)
                            .folderName(newFolderName)
                            .folderDescription("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZr")
                            .build()
                    ),
                    Arguments.of(FileUpdateDto.builder()
                            .fileName(newFileName)
                            .fileDescription(newFileDescription)
                            .folderName("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZr")
                            .folderDescription(newFolderDescription)
                            .build()
                    ),
                    Arguments.of(FileUpdateDto.builder()
                            .fileName("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZr.uisi")
                            .fileDescription(newFileDescription)
                            .folderName("rDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZrDdylCoyZDkYnipbiycCnUvGFUGQcHfpnZr")
                            .folderDescription(newFolderDescription)
                            .build()
                    )
            );
        }

        @Test
        public void update_file_info_status_400_when_file_extension_is_wrong() throws Exception {
            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileId(uploadFIleDto.getFileId())
                    .fileName("textfile.test")
                    .fileDescription(TEST_FILE_DESCRIPTION)
                    .folderName(TEST_FOLDER_NAME)
                    .folderDescription(TEST_FOLDER_DESCRIPTION)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void update_file_info_status_401_when_user_is_not_authorized() throws Exception {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";

            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileId(uploadFIleDto.getFileId())
                    .fileName(newFileName)
                    .fileDescription(newFileDescription)
                    .folderName(newFolderName)
                    .folderDescription(newFolderDescription)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "testtest"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void update_file_info_status_403_when_user_dont_have_right() throws Exception {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";

            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileId(uploadFIleDto.getFileId())
                    .fileName(newFileName)
                    .fileDescription(newFileDescription)
                    .folderName(newFolderName)
                    .folderDescription(newFolderDescription)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            loginUser2();

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void update_file_info_status_404_when_file_not_found() throws Exception {
            String newFileName = "new_file.txt";
            String newFileDescription = "New file description";
            String newFolderName = "newfolder";
            String newFolderDescription = "New folder description";

            FileUpdateDto dto = FileUpdateDto.builder()
                    .fileId(UUID.randomUUID())
                    .fileName(newFileName)
                    .fileDescription(newFileDescription)
                    .folderName(newFolderName)
                    .folderDescription(newFolderDescription)
                    .build();
            String jsonDto = mapper.writeValueAsString(dto);

            mockMvc.perform(put(PUT_FILE_INFO_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonDto)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }
    }

    @Nested
    @DisplayName("DELETE /v1/file{id}")
    class DeleteFileTests {

        private static FileResponseDto uploadFIleDto = new FileResponseDto();

        @BeforeEach
        void setUp() throws Exception {
            Random random = new Random();
            uploadFIleDto = uploadTextFile(TEST_TXT_FILE_NAME,
                    "This is the content of the test file",
                    TEST_FILE_DESCRIPTION,
                    TEST_FOLDER_NAME,
                    TEST_FOLDER_DESCRIPTION,
                    FOLDER_PATH,
                    accessToken1,
                    random.nextBoolean());
        }

        @Test
        public void delete_file_status_204() throws Exception {
            UUID fileId = uploadFIleDto.getFileId();
            Folder folder = fileService.findOriginalFileInfoById(fileId).getFolder();
            List<FileInfo> filesInfos = fileService.findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), fileId);

            mockMvc.perform(delete(DELETE_FILE_BY_ID_PATH, fileId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNoContent());

            assertThrows(FileInfoNotFoundException.class, () -> fileService.findOriginalFileInfoById(fileId));
            filesInfos.forEach(fi ->
                    assertFalse(dataStorageService.isObjectExist(fi.getPath(), fi.getBucketName()))
            );
        }

        @Test
        public void delete_file_status_204_when_user_is_admin() throws Exception {

            UUID fileId = uploadFIleDto.getFileId();
            Folder folder = fileService.findOriginalFileInfoById(fileId).getFolder();
            List<FileInfo> filesInfos = fileService.findFilesInfosByFolderIdAndOriginalFileId(folder.getId(), fileId);

            loginAdmin();
            mockMvc.perform(delete(DELETE_FILE_BY_ID_PATH, fileId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken))
                    .andExpect(status().isNoContent());

            assertThrows(FileInfoNotFoundException.class, () -> fileService.findOriginalFileInfoById(fileId));
            filesInfos.forEach(fi ->
                    assertFalse(dataStorageService.isObjectExist(fi.getPath(), fi.getBucketName()))
            );
        }

        @Test
        public void delete_file_status_400_when_id_is_wnong() throws Exception {
            mockMvc.perform(get(DELETE_FILE_BY_ID_PATH, "123")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void delete_file_status_401_user_is_not_authorized() throws Exception {
            mockMvc.perform(get(DELETE_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + "kdkdkkkdkd"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void delete_file_status_403_user_dont_have_right() throws Exception {
            loginUser2();
            mockMvc.perform(get(DELETE_FILE_BY_ID_PATH, uploadFIleDto.getFileId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @Test
        public void delete_file_status_404_when_file_not_found() throws Exception {
            mockMvc.perform(get(DELETE_FILE_BY_ID_PATH, UUID.randomUUID())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }
    }
}
