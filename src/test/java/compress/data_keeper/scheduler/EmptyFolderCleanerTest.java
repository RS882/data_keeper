package compress.data_keeper.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import compress.data_keeper.domain.dto.files.FileDto;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.folders.FolderDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.EntityInfo;
import compress.data_keeper.domain.entity.FileInfo;
import compress.data_keeper.domain.entity.Folder;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.repository.FileInfoRepository;
import compress.data_keeper.repository.FolderRepository;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.interfaces.DataStorageService;
import compress.data_keeper.services.interfaces.FileService;
import compress.data_keeper.services.interfaces.FolderService;
import compress.data_keeper.services.interfaces.UserService;
import compress.data_keeper.services.mapping.UserMapperService;
import jakarta.persistence.*;
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
class EmptyFolderCleanerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private EmptyFolderCleaner emptyFolderCleaner;

    @Autowired
    private DataStorageService dataStorageService;

    @Autowired
    private UserMapperService mapperService;

    @Value("${bucket.temp}")
    private String tempBucketName;

    @Value("${bucket.name}")
    private String bucketName;

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
    private User user1;

    private String accessToken2;
    private Long currentUserId2;
    private User user2;

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
        loginUser2();
    }

    private void loginUser1() throws Exception {
        TokenResponseDto responseDto = loginUser(USER1_EMAIL, TEST_USER_NAME_1, USER1_PASSWORD);
        accessToken1 = responseDto.getAccessToken();
        currentUserId1 = responseDto.getUserId();
        user1 = userRepository.findById(currentUserId1).orElse(null);
    }

    private void loginUser2() throws Exception {
        TokenResponseDto responseDto = loginUser(USER2_EMAIL, TEST_USER_NAME_2, USER2_PASSWORD);
        accessToken2 = responseDto.getAccessToken();
        currentUserId2 = responseDto.getUserId();
        user2 = userRepository.findById(currentUserId1).orElse(null);
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

    private List<Folder> createFolders(String bucketName, boolean isProtected, boolean isTemp, User user, int countOfFolders) throws Exception {
        List<Folder> folders = new ArrayList<>();
        for (int i = 0; i < countOfFolders; i++) {
            Folder folder = Folder.builder()
                    .name("test folder name " + i)
                    .description("test folder description " + i)
                    .bucketName(bucketName)
                    .isProtected(isProtected)
                    .isTemp(isTemp)
                    .owner(user)
                    .build();
            folders.add(folder);
        }
        return folderRepository.saveAll(folders);
    }

    @Nested
    @DisplayName("Scheduler delete empty folder when folder isn't protected")
    class DeleteEmptyFolder {

        @Test
        @Transactional
        public void delete_empty_unprotected_folder_positive_test() throws Exception {
            List<FileResponseDto> allFileResponseDtos = new ArrayList<>();
            List<FileResponseDto> uploadUser1ToTempBefore =
                    uploadAndMoveSomeFileForTest(5, accessToken1, false);
            allFileResponseDtos.addAll(uploadUser1ToTempBefore);
            List<FileResponseDto> uploadUser2ToTempBefore =
                    uploadAndMoveSomeFileForTest(3, accessToken2, false);
            allFileResponseDtos.addAll(uploadUser2ToTempBefore);
            List<FileResponseDto> uploadUser1ToStorageBefore =
                    uploadAndMoveSomeFileForTest(4, accessToken1, true);
            allFileResponseDtos.addAll(uploadUser1ToStorageBefore);
            List<FileResponseDto> uploadUser2ToStorageBefore =
                    uploadAndMoveSomeFileForTest(6, accessToken2, true);
            allFileResponseDtos.addAll(uploadUser2ToStorageBefore);

            List<Folder> dontEmptyFolders = allFileResponseDtos.stream().map(f ->
                            Objects.requireNonNull(fileInfoRepository.findById(f.getFileId()).orElse(null)).getFolder()
                    )
                    .distinct()
                    .toList();
            List<Folder> willNotBeDeletedFolders =new ArrayList<>(dontEmptyFolders);
            List<Folder> foldersForDelete = new ArrayList<>();

            foldersForDelete.addAll(
                    createFolders(tempBucketName, false, true, user1, 5));
            foldersForDelete.addAll(
                    createFolders(tempBucketName, true, true, user1, 5));
            foldersForDelete.addAll(
                    createFolders(tempBucketName, false, false, user1, 5));
            willNotBeDeletedFolders.addAll(
                    createFolders(tempBucketName, true, false, user1, 5));

            foldersForDelete.addAll(
                    createFolders(tempBucketName, false, true, user2, 5));
            foldersForDelete.addAll(
                    createFolders(tempBucketName, true, true, user2, 5));
            foldersForDelete.addAll(
                    createFolders(tempBucketName, false, false, user2, 5));
            willNotBeDeletedFolders.addAll(
                    createFolders(tempBucketName, true, false, user2, 5));

            foldersForDelete.addAll(
                    createFolders(bucketName, false, true, user1, 5));
            foldersForDelete.addAll(
                    createFolders(bucketName, true, true, user1, 5));
            foldersForDelete.addAll(
                    createFolders(bucketName, false, false, user1, 5));
            willNotBeDeletedFolders.addAll(
                    createFolders(bucketName, true, false, user1, 5));

            foldersForDelete.addAll(
                    createFolders(bucketName, false, true, user2, 5));
            foldersForDelete.addAll(
                    createFolders(bucketName, true, true, user2, 5));
            foldersForDelete.addAll(
                    createFolders(bucketName, false, false, user2, 5));
            willNotBeDeletedFolders.addAll(
                    createFolders(bucketName, true, false, user2, 5));


            emptyFolderCleaner.deleteEmptyFolders();

            List<Folder> folders = folderRepository.findAll();
            assertEquals(folders,willNotBeDeletedFolders);

            List<UUID> deletedFoldersId = foldersForDelete.stream().map(EntityInfo::getId).toList();
            List<Folder> deletedFolder = folderRepository.findAllById(deletedFoldersId);
            assertEquals(deletedFolder.size(), 0);
        }

        @Test
        @Transactional
        public void delete_empty_unprotected_folder_positive_test_when_dont_have_empty_folder() throws Exception {
            List<FileResponseDto> allFileResponseDtos = new ArrayList<>();
            List<FileResponseDto> uploadUser1ToTempBefore =
                    uploadAndMoveSomeFileForTest(5, accessToken1, false);
            allFileResponseDtos.addAll(uploadUser1ToTempBefore);
            List<FileResponseDto> uploadUser2ToTempBefore =
                    uploadAndMoveSomeFileForTest(3, accessToken2, false);
            allFileResponseDtos.addAll(uploadUser2ToTempBefore);
            List<FileResponseDto> uploadUser1ToStorageBefore =
                    uploadAndMoveSomeFileForTest(4, accessToken1, true);
            allFileResponseDtos.addAll(uploadUser1ToStorageBefore);
            List<FileResponseDto> uploadUser2ToStorageBefore =
                    uploadAndMoveSomeFileForTest(6, accessToken2, true);
            allFileResponseDtos.addAll(uploadUser2ToStorageBefore);

            List<Folder> dontEmptyFolders = allFileResponseDtos.stream().map(f ->
                            Objects.requireNonNull(fileInfoRepository.findById(f.getFileId()).orElse(null)).getFolder()
                    )
                    .distinct()
                    .toList();
            List<Folder> willNotBeDeletedFolders =new ArrayList<>(dontEmptyFolders);

            willNotBeDeletedFolders.addAll(
                    createFolders(tempBucketName, true, false, user1, 5));

            willNotBeDeletedFolders.addAll(
                    createFolders(tempBucketName, true, false, user2, 5));

            willNotBeDeletedFolders.addAll(
                    createFolders(bucketName, true, false, user1, 5));

            willNotBeDeletedFolders.addAll(
                    createFolders(bucketName, true, false, user2, 5));


            emptyFolderCleaner.deleteEmptyFolders();

            List<Folder> folders = folderRepository.findAll();
            assertEquals(folders,willNotBeDeletedFolders);
        }
    }
}