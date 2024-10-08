//package compress.data_keeper.security.controllers;
//
//import compress.data_keeper.security.contstants.Role;
//import compress.data_keeper.security.domain.dto.LoginDto;
//import compress.data_keeper.security.domain.dto.TokenResponseDto;
//import compress.data_keeper.security.domain.dto.ValidationResponseDto;
//import compress.data_keeper.security.domain.entity.User;
//import compress.data_keeper.security.repositorys.TokenRepository;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.*;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.web.client.ResourceAccessException;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.stream.Stream;
//
//import static compress.data_keeper.security.services.AuthServiceImpl.MAX_COUNT_OF_LOGINS;
//import static compress.data_keeper.security.services.CookieService.COOKIE_REFRESH_TOKEN_NAME;
//import static org.junit.jupiter.api.Assertions.*;
//
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class AuthTest {
//
//
//    @LocalServerPort
//    private int port;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private TokenRepository tokenRepository;
//
//    private TestRestTemplate template;
//    private HttpHeaders headers;
//
//    private String user1Id;
//
//    private static final String USER1_EMAIL = "Test_user_123456@example.com";
//    private static final String USER1_PASSWORD = "Querty123!";
//    private final String URL_PREFIX = "http://localhost:";
//    private final String AUTH_RESOURCE_NAME = "/api/v1/auth";
//    private final String LOGIN_URL = "/login";
//    private final String REFRESH_URL = "/refresh";
//    private final String VALIDATION_URL = "/validation";
//    private final String LOGOUT_URL = "/logout";
//
//    private final String BEARER_PREFIX = "Bearer ";
//
//    private LoginDto dto;
//
//    @BeforeEach
//    public void setUp() {
//        headers = new HttpHeaders();
//        template = new TestRestTemplate();
//
//        dto = LoginDto.builder()
//                .email(USER1_EMAIL)
//                .password(USER1_PASSWORD)
//                .build();
//
//        User user = userRepository.findByEmailAndIsActiveTrue(USER1_EMAIL).orElse(null);
//
//        if (user == null) {
//            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(8);
//
//            User testUser = User.builder()
//                    .email(USER1_EMAIL)
//                    .password(encoder.encode(USER1_PASSWORD))
//                    .isActive(true)
//                    .role(Role.ROLE_USER)
//                    .loginBlockedUntil(LocalDateTime.now())
//                    .build();
//            user1Id = userRepository.save(testUser).getId();
//        } else user1Id = user.getId();
//    }
//
//    @AfterEach
//    public void tearDown() {
//        userRepository.deleteAllByEmail(USER1_EMAIL);
//        tokenRepository.deleteAllByUserId(user1Id);
//    }
//
//    private String getCookieValue(String cookie) {
//        String cookieTitle = COOKIE_REFRESH_TOKEN_NAME + "=";
//        String[] parts = cookie.split(";");
//        for (String part : parts) {
//            if (part.startsWith(cookieTitle)) {
//                return part.substring(cookieTitle.length());
//            }
//        }
//        return null;
//    }
//
//    private ResponseEntity<TokenResponseDto> loginTestUser() {
//        return loginTestUser(this.dto);
//    }
//
//    private ResponseEntity<TokenResponseDto> loginTestUser(LoginDto dto) {
//        HttpEntity<LoginDto> request = new HttpEntity<>(dto, headers);
//        final String URL_LOGIN = URL_PREFIX + port + AUTH_RESOURCE_NAME + LOGIN_URL;
//        return template.postForEntity(URL_LOGIN, request, TokenResponseDto.class);
//    }
//
//    private <T> void _401_header_authorization_is_null(String url, Class<T> responseType) {
//        HttpEntity<Object> requestValid = new HttpEntity<>(headers);
//        ResponseEntity<T> responseValid =
//                template.exchange(url, HttpMethod.GET, requestValid, responseType);
//        assertEquals(HttpStatus.UNAUTHORIZED, responseValid.getStatusCode(), "Refresh token not found");
//    }
//
//    private <T> void _401_header_authorization_is_not_bearer(String url, Class<T> responseType) {
//
//        ResponseEntity<TokenResponseDto> response = loginTestUser();
//
//        String accessToken = "Test " + response.getBody().getAccessToken();
//
//        headers.set("Content-Type", "application/json");
//        headers.set(HttpHeaders.AUTHORIZATION, accessToken);
//
//        HttpEntity<Object> requestValid = new HttpEntity<>(headers);
//        ResponseEntity<T> responseValid =
//                template.exchange(url, HttpMethod.GET, requestValid, responseType);
//        assertEquals(HttpStatus.UNAUTHORIZED, responseValid.getStatusCode(), "Refresh token not found");
//    }
//
//    private <T> void _401_token_is_incorrect(String url, Class<T> responseType) {
//
//        String accessToken = BEARER_PREFIX + "WRonG!TeSt%TokeN234";
//
//        headers.set("Content-Type", "application/json");
//        headers.set(HttpHeaders.AUTHORIZATION, accessToken);
//
//        HttpEntity<Object> requestValid = new HttpEntity<>(headers);
//        ResponseEntity<T> responseValid =
//                template.exchange(url, HttpMethod.GET, requestValid, responseType);
//        assertEquals(HttpStatus.UNAUTHORIZED, responseValid.getStatusCode(), "Refresh token not found");
//    }
//
//
//    @Nested
//    @DisplayName("POST /v1/auth/login")
//    class Login {
//
//        private String URL = URL_PREFIX + port + AUTH_RESOURCE_NAME + LOGIN_URL;
//
//        @Test
//        public void login_with_status_200() {
//
//            ResponseEntity<TokenResponseDto> response = loginTestUser();
//
//            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
//            String refreshToken = null;
//            if (cookies != null) {
//                for (String cookie : cookies) {
//                    if (cookie.startsWith(COOKIE_REFRESH_TOKEN_NAME))
//                        refreshToken = getCookieValue(cookie);
//                }
//            }
//
//            assertNotNull(refreshToken, "Refresh token not found");
//            assertEquals(HttpStatus.OK, response.getStatusCode(), "Response has unexpected status");
//            assertTrue(response.hasBody(), "Response does not contain body");
//            assertNotNull(Objects.requireNonNull(response.getBody()).getAccessToken(), "AccessToken not found");
//        }
//
//        @ParameterizedTest(name = "Тест {index}: login_with_status_400_login_data_is_incorrect [{arguments}]")
//        @MethodSource("incorrectLoginData")
//        public void login_with_status_400_login_data_is_incorrect(LoginDto dto) {
//            ResponseEntity<TokenResponseDto> response = loginTestUser(dto);
//            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Response has unexpected status");
//        }
//
//
//        @ParameterizedTest(name = "Test {index}: login_with_status_404_email_or_password_is_wrong [{arguments}]")
//        @MethodSource("wrongLoginData")
//        public void login_with_status_401_email_or_password_is_wrong(LoginDto dto) {
//            try {
//                ResponseEntity<TokenResponseDto> response = loginTestUser(dto);
//                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "Response has unexpected status");
//            } catch (ResourceAccessException ex) {
//                assertTrue(ex.getMessage() != null);
//            }
//        }
//
//        @Test
//        public void login_with_status_403_count_of_logins_is_more_than_maximum() {
//            HttpEntity<LoginDto> request = new HttpEntity<>(dto, headers);
//            for (int i = 0; i < MAX_COUNT_OF_LOGINS + 1; i++) {
//                template.postForEntity(URL, request, TokenResponseDto.class);
//            }
//            ResponseEntity<TokenResponseDto> response =
//                    template.postForEntity(URL, request, TokenResponseDto.class);
//            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Response has unexpected status");
//        }
//
//        private static Stream<Arguments> incorrectLoginData() {
//            return Stream.of(Arguments.of(
//                            LoginDto.builder()
//                                    .email("testexample?com")
//                                    .password(USER1_PASSWORD)
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .password(USER1_PASSWORD)
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("1E")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("asdasdlDFsd90q!u023402lks@djalsdajsd#lahsdkahs$$%dllkasd")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("asdasdlweqwe")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("asda@sdlweqwe")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("asdasdlwe8qwe")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("Qsdasdlwe8qwe")
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email("testexample?com")
//                                    .password("Qsdasdlwe8qwe")
//                                    .build())
//            );
//        }
//
//        private static Stream<Arguments> wrongLoginData() {
//            return Stream.of(
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(UUID.randomUUID() + "@example.com")
//                                    .password(USER1_PASSWORD)
//                                    .build()),
//                    Arguments.of(
//                            LoginDto.builder()
//                                    .email(USER1_EMAIL)
//                                    .password("doIusjn%70")
//                                    .build())
//            );
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /v1/auth/refresh")
//    class Refresh {
//
//        private String URL = URL_PREFIX + port + AUTH_RESOURCE_NAME + REFRESH_URL;
//
//        @Test
//        public void refresh_with_status_200() {
//
//            ResponseEntity<TokenResponseDto> response = loginTestUser();
//
//            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
//
//            String refreshToken = null;
//            if (cookies != null) {
//                for (String cookie : cookies) {
//                    if (cookie.startsWith(COOKIE_REFRESH_TOKEN_NAME))
//                        refreshToken = cookie;
//                }
//            }
//            headers.add(HttpHeaders.COOKIE, refreshToken);
//
//            headers.add("Set-Cookie", COOKIE_REFRESH_TOKEN_NAME + "=" + refreshToken + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60));
//            headers.set("Content-Type", "application/json");
//
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<TokenResponseDto> responseRefresh =
//                    template.exchange(URL, HttpMethod.GET, httpEntity, TokenResponseDto.class);
//
//            List<String> cookiesNew = responseRefresh.getHeaders().get(HttpHeaders.SET_COOKIE);
//            String refreshTokenNew = null;
//            if (cookiesNew != null) {
//                for (String cookie : cookiesNew) {
//                    if (cookie.startsWith(COOKIE_REFRESH_TOKEN_NAME))
//                        refreshTokenNew = getCookieValue(cookie);
//                }
//            }
//            assertNotNull(refreshTokenNew, "Refresh token not found");
//            assertEquals(HttpStatus.OK, responseRefresh.getStatusCode(), "Response has unexpected status");
//            assertTrue(responseRefresh.hasBody(), "Response does not contain body");
//            assertNotNull(Objects.requireNonNull(responseRefresh.getBody()).getAccessToken(), "AccessToken not found");
//        }
//
//        @Test
//        public void refresh_with_status_400_cookie_is_null() {
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//            ResponseEntity<TokenResponseDto> responseRefresh =
//                    template.exchange(URL, HttpMethod.GET, httpEntity, TokenResponseDto.class);
//            assertEquals(HttpStatus.BAD_REQUEST, responseRefresh.getStatusCode(), "Response has unexpected status");
//        }
//
//        @Test
//        public void refresh_with_status_400_cookie_is_incorrect() {
//            String refreshToken = "test";
//            headers.add("test", refreshToken);
//
//            headers.add("Set-Cookie", "test" + "=" + refreshToken + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60));
//            headers.set("Content-Type", "application/json");
//
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<TokenResponseDto> responseRefresh =
//                    template.exchange(URL, HttpMethod.GET, httpEntity, TokenResponseDto.class);
//            assertEquals(HttpStatus.BAD_REQUEST, responseRefresh.getStatusCode(), "Response has unexpected status");
//        }
//
//        @Test
//        public void refresh_with_status_401_token_is_incorrect() {
//            String refreshToken = COOKIE_REFRESH_TOKEN_NAME + "=" + "test" + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60);
//            headers.add(HttpHeaders.COOKIE, refreshToken);
//
//            headers.add("Set-Cookie", COOKIE_REFRESH_TOKEN_NAME + "=" + refreshToken + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60));
//            headers.set("Content-Type", "application/json");
//
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<TokenResponseDto> responseRefresh =
//                    template.exchange(URL, HttpMethod.GET, httpEntity, TokenResponseDto.class);
//            assertEquals(HttpStatus.UNAUTHORIZED, responseRefresh.getStatusCode(), "Response has unexpected status");
//        }
//
//        @Test
//        public void refresh_with_status_401_token_is_invalid() {
//
//            ResponseEntity<TokenResponseDto> response = loginTestUser(dto);
//
//            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
//
//            String newCookie = null;
//            if (cookies != null) {
//                for (String cookie : cookies) {
//                    if (cookie.startsWith(COOKIE_REFRESH_TOKEN_NAME))
//                        newCookie = cookie;
//                }
//            }
//
//            tokenRepository.deleteAllByToken(getCookieValue(newCookie));
//
//            headers.add(HttpHeaders.COOKIE, newCookie);
//
//            headers.add("Set-Cookie", COOKIE_REFRESH_TOKEN_NAME + "=" + newCookie + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60));
//            headers.set("Content-Type", "application/json");
//
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<TokenResponseDto> responseRefresh =
//                    template.exchange(URL, HttpMethod.GET, httpEntity, TokenResponseDto.class);
//            assertEquals(HttpStatus.UNAUTHORIZED, responseRefresh.getStatusCode(), "Response has unexpected status");
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /v1/auth/validation")
//    class Validation {
//        private final String URL = URL_PREFIX + port + AUTH_RESOURCE_NAME + VALIDATION_URL;
//
//        @Test
//        public void validation_with_status_200() {
//
//            ResponseEntity<TokenResponseDto> response = loginTestUser(dto);
//
//            String accessToken = BEARER_PREFIX + response.getBody().getAccessToken();
//
//            headers.set("Content-Type", "application/json");
//            headers.set(HttpHeaders.AUTHORIZATION, accessToken);
//
//            HttpEntity<Object> requestValid = new HttpEntity<>(headers);
//            ResponseEntity<ValidationResponseDto> responseValid =
//                    template.exchange(URL, HttpMethod.GET, requestValid, ValidationResponseDto.class);
//
//            assertEquals(HttpStatus.OK, responseValid.getStatusCode(), "Refresh token not found");
//        }
//
//        @Test
//        public void validation_with_status_401_header_authorization_is_null() {
//            _401_header_authorization_is_null(URL, ValidationResponseDto.class);
//        }
//
//        @Test
//        public void validation_with_status_401_header_authorization_is_not_bearer() {
//            _401_header_authorization_is_not_bearer(URL, ValidationResponseDto.class);
//        }
//
//        @Test
//        public void validation_with_status_401_token_is_incorrect() {
//            _401_token_is_incorrect(URL, ValidationResponseDto.class);
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /v1/auth/logout")
//    class Logout {
//
//        private String URL = URL_PREFIX + port + AUTH_RESOURCE_NAME + LOGOUT_URL;
//
//        @Test
//        public void logout_with_status_200() {
//            ResponseEntity<TokenResponseDto> response = loginTestUser();
//
//            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
//
//            String accessToken = BEARER_PREFIX + response.getBody().getAccessToken();
//
//            String refreshToken = null;
//            if (cookies != null) {
//                for (String cookie : cookies) {
//                    if (cookie.startsWith(COOKIE_REFRESH_TOKEN_NAME))
//                        refreshToken = cookie;
//                }
//            }
//            headers.add(HttpHeaders.COOKIE, refreshToken);
//
//            headers.add("Set-Cookie", COOKIE_REFRESH_TOKEN_NAME + "=" + refreshToken + "; Path=/; HttpOnly; Secure; Max-Age=" + (15 * 60));
//            headers.set("Content-Type", "application/json");
//            headers.set(HttpHeaders.AUTHORIZATION, accessToken);
//
//            HttpEntity<Object> requestValid = new HttpEntity<>(headers);
//
//            ResponseEntity<Object> responseValid =
//                    template.exchange(URL, HttpMethod.GET, requestValid, (Class<Object>) null);
//            assertEquals(HttpStatus.OK, responseValid.getStatusCode(), "Refresh token not found");
//
//        }
//
//        @Test
//        public void logout_with_status_401_header_authorization_is_null() {
//            _401_header_authorization_is_null(URL, null);
//        }
//
//        @Test
//        public void logout_with_status_401_header_authorization_is_not_bearer() {
//            _401_header_authorization_is_not_bearer(URL, null);
//        }
//
//        @Test
//        public void logout_with_status_401_token_is_incorrect() {
//            _401_token_is_incorrect(URL, null);
//        }
//    }
//}
