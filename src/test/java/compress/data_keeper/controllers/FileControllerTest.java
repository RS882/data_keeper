package compress.data_keeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import compress.data_keeper.domain.dto.files.FileResponseDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.services.mapping.UserMapperService;
import compress.data_keeper.services.utilities.FileUtilities;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static compress.data_keeper.constants.ImgConstants.IMAGE_SIZES;
import static compress.data_keeper.domain.dto.files.FileResponseDto.ORIGINAL_FILE_KEY;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private ObjectMapper mapper = new ObjectMapper();

    private String accessToken1;

    private static final String USER1_EMAIL = "Test" + "@example.com";
    private static final String USER1_PASSWORD = "Querty123!";
    private static final String TEST_USER_NAME_1 = "TestName1";

    private static final String FILE_TEMP_LOAD_PATH = "/v1/file/temp";
    private final String LOGIN_URL = "/v1/auth/login";

    @BeforeEach
    void setUp() throws Exception {
        UserRegistrationDto dto = UserRegistrationDto
                .builder()
                .email(USER1_EMAIL)
                .userName(TEST_USER_NAME_1)
                .password(USER1_PASSWORD)
                .build();

        userRepository.save(mapperService.toEntity(dto));

        String dtoJson = mapper.writeValueAsString(
                LoginDto.builder()
                        .email(USER1_EMAIL)
                        .password(USER1_PASSWORD)
                        .build());
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        TokenResponseDto dto2 = mapper.readValue(jsonResponse, TokenResponseDto.class);
        accessToken1 = dto2.getAccessToken();
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

            String originalFileLink = responseDto.getLinksToFiles().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFileLink);
            assertInstanceOf(String.class, originalFileLink);

            String originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFilePath);
            assertInstanceOf(String.class, originalFilePath);

            Set<String> sizes = IMAGE_SIZES.stream().map(FileUtilities::getNameFromSizes).collect(Collectors.toSet());

            sizes.forEach(s -> {
                String linkValue = responseDto.getLinksToFiles().get(s);
                assertNotNull(linkValue);
                assertInstanceOf(String.class, linkValue);
                String pathValue = responseDto.getPaths().get(s);
                assertNotNull(pathValue);
                assertInstanceOf(String.class, pathValue);
            });
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

            String originalFileLink = responseDto.getLinksToFiles().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFileLink);
            assertInstanceOf(String.class, originalFileLink);

            String originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFilePath);
            assertInstanceOf(String.class, originalFilePath);

            Set<String> sizes = IMAGE_SIZES.stream().map(FileUtilities::getNameFromSizes).collect(Collectors.toSet());

            sizes.forEach(s -> {
                String linkValue = responseDto.getLinksToFiles().get(s);
                assertNotNull(linkValue);
                assertInstanceOf(String.class, linkValue);
                String pathValue = responseDto.getPaths().get(s);
                assertNotNull(pathValue);
                assertInstanceOf(String.class, pathValue);
            });
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

            String originalFileLink2 = responseDto2.getLinksToFiles().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFileLink2);
            assertInstanceOf(String.class, originalFileLink2);

            String originalFilePath2 = responseDto2.getPaths().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFilePath2);
            assertInstanceOf(String.class, originalFilePath2);

            Set<String> sizes = IMAGE_SIZES.stream().map(FileUtilities::getNameFromSizes).collect(Collectors.toSet());

            sizes.forEach(s -> {
                String linkValue = responseDto2.getLinksToFiles().get(s);
                assertNotNull(linkValue);
                assertInstanceOf(String.class, linkValue);
                String pathValue = responseDto2.getPaths().get(s);
                assertNotNull(pathValue);
                assertInstanceOf(String.class, pathValue);
            });
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

            String originalFileLink = responseDto.getLinksToFiles().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFileLink);
            assertInstanceOf(String.class, originalFileLink);

            String originalFilePath = responseDto.getPaths().get(ORIGINAL_FILE_KEY);
            assertNotNull(originalFilePath);
            assertInstanceOf(String.class, originalFilePath);

            Set<String> sizes = IMAGE_SIZES.stream().map(FileUtilities::getNameFromSizes).collect(Collectors.toSet());

            sizes.forEach(s -> {
                String linkValue = responseDto.getLinksToFiles().get(s);
                assertNull(linkValue);
                String pathValue = responseDto.getPaths().get(s);
                assertNull(pathValue);
            });
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
                            .file(mockFile)  // предполагается, что mockFile пустой
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
    }
}