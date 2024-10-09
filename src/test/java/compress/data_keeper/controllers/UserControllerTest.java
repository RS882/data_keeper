package compress.data_keeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User controller integration tests: ")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
@Transactional
@Rollback
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper = new ObjectMapper();

    private final String USER_REGISTRATION_PATH = "/v1/user/registration";

    private static final String TEST_USER_NAME_1 = "TestName1";
    private static final String TEST_USER_NAME_2 = "TestName2";
    private static final String TEST_USER_EMAIL_1 = "test.user1@test.com";
    private static final String TEST_USER_PASSWORD_1 = "qwerty!123";
    private static final String TEST_USER_PASSWORD_2 = "jwerty!123";

    @Nested
    @DisplayName("POST /v1/user/registration")
    class RegistrationUserTest {

        @Test
        public void createUser_with_status_201() throws Exception {
            String dtoJson = mapper.writeValueAsString(UserRegistrationDto.builder()
                    .email(TEST_USER_EMAIL_1)
                    .userName(TEST_USER_NAME_1)
                    .password(TEST_USER_PASSWORD_1)
                    .build());
            mockMvc.perform(post(USER_REGISTRATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(dtoJson))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userName", is(TEST_USER_NAME_1)))
                    .andExpect(jsonPath("$.email", is(TEST_USER_EMAIL_1)))
                    .andExpect(jsonPath("$.userId", isA(Number.class)))
                    .andExpect(jsonPath("$.userId", greaterThanOrEqualTo(0)));
        }

        @Test
        public void createUser_with_status_400_email_address_already_in_use() throws Exception {
            String dtoJson = mapper.writeValueAsString(UserRegistrationDto.builder()
                    .email(TEST_USER_EMAIL_1)
                    .userName(TEST_USER_NAME_1)
                    .password(TEST_USER_PASSWORD_1)
                    .build());

            mockMvc.perform(post(USER_REGISTRATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(dtoJson))
                    .andExpect(status().isCreated());

            String dtoJson2 = mapper.writeValueAsString(UserRegistrationDto.builder()
                    .email(TEST_USER_EMAIL_1)
                    .userName(TEST_USER_NAME_2)
                    .password(TEST_USER_PASSWORD_2)
                    .build());

            mockMvc.perform(post(USER_REGISTRATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(dtoJson2))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", isA(String.class)));
        }

        @ParameterizedTest(name = "Тест {index}: createUser_with_status_400_registration_data_is_incorrect [{arguments}]")
        @MethodSource("incorrectRegistrationData")
        public void createUser_with_status_400_registration_data_is_incorrect(UserRegistrationDto dto) throws Exception {
            String dtoJson2 = mapper.writeValueAsString(dto);

            mockMvc.perform(post(USER_REGISTRATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(dtoJson2))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }

        private static Stream<Arguments> incorrectRegistrationData() {
            return Stream.of(Arguments.of(
                            UserRegistrationDto.builder()
                                    .email("testexample?com")
                                    .userName(TEST_USER_NAME_1)
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email("testexample23om")
                                    .userName(TEST_USER_NAME_1)
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .userName(TEST_USER_NAME_1)
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName("uI")
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName("uIasdsdasfsdf_Asid2032o4p12o3joefhsodfhosdhf898ihoih2434efg34grfgdfgsdfasdposapfjsddfhg")
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .password(TEST_USER_PASSWORD_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("1E")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("asdasdlDFsd90q!u023402lks@djalsdajsd#lahsdkahs$$%dllkasd")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("asdasdlweqwe")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("asda@sdlweqwe")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("asdasdlwe8qwe")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .password("Qsdasdlwe8qwe")
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email(TEST_USER_EMAIL_1)
                                    .userName(TEST_USER_NAME_1)
                                    .build()),
                    Arguments.of(
                            UserRegistrationDto.builder()
                                    .email("testexample?com")
                                    .userName("2y")
                                    .password("Qsdasdlwe8qwe")
                                    .build())
            );
        }
    }
}