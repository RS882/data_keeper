package compress.data_keeper.security.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.exception_handler.authentication_exception.WrongTokenException;
import compress.data_keeper.exception_handler.forbidden.exceptions.LimitOfLoginsException;
import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.security.domain.dto.TokensDto;
import compress.data_keeper.security.domain.dto.ValidationResponseDto;
import compress.data_keeper.security.services.interfaces.AuthService;
import compress.data_keeper.security.services.mapping.TokenDtoMapperService;
import compress.data_keeper.services.interfaces.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static compress.data_keeper.security.services.TokenService.USER_ROLE_VARIABLE_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final TokenDtoMapperService tokenDtoMapperService;

    public static final int MAX_COUNT_OF_LOGINS = 5;

    @Override
    public TokensDto login(LoginDto loginDto, User currentUser) {
        checkLoginBlockedTime(currentUser);
        if (!encoder.matches(loginDto.getPassword(), currentUser.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }
        setLoginBlockedTime(currentUser);
        return tokenService.getTokens(currentUser);
    }

    @Override
    public TokensDto refresh(String inboundRefreshToken) {

        if (!tokenService.validateRefreshToken(inboundRefreshToken))
            throw new WrongTokenException("Token is incorrect");

        Claims claims = tokenService.getRefreshTokenClaims(inboundRefreshToken);
        User user = userService.getUserByEmail(claims.getSubject());
        List<String> refreshTokens = tokenService.getRefreshTokensByUserId(user.getId());

        if (!refreshTokens.contains(inboundRefreshToken))
            throw new WrongTokenException("Token is wrong");

        TokensDto tokensDto = tokenService.getTokens(user);
        tokenService.removeOldRefreshToken(inboundRefreshToken);
        return tokensDto;
    }

    @Override
    public ValidationResponseDto validation(String authorizationHeader) {

        String token = authorizationHeader.substring(7);
        Claims claims = tokenService.getAccessTokenClaims(token);
        User user = userService.getUserByEmail(claims.getSubject());

        return ValidationResponseDto.builder()
                .isAuthorized(true)
                .roles((List) claims.get(USER_ROLE_VARIABLE_NAME))
                .userId(user.getId())
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.removeOldRefreshToken(refreshToken);
        SecurityContextHolder.clearContext();
    }

    @Override
    public TokenResponseDto getTokenResponseDto(TokensDto tokensDto) {
        return tokenDtoMapperService.toResponseDto(tokensDto);
    }

    private void setLoginBlockedTime(User user) {
        Long userId = user.getId();
        List<String> refreshTokens = tokenService.getRefreshTokensByUserId(userId);

        if (refreshTokens.size() >= MAX_COUNT_OF_LOGINS) {

            refreshTokens.forEach(tokenService::removeOldRefreshToken);

            user.setLoginBlockedUntil(LocalDateTime.now().plusMinutes(5));
            userService.saveUser(user);

            log.warn("User {} has limit of logins :{}.", userId,MAX_COUNT_OF_LOGINS);
            log.warn("User {} logins blocked until:{}.", userId,user.getLoginBlockedUntil());
        }
    }

    private void checkLoginBlockedTime(User user) {
        if (user.getLoginBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new LimitOfLoginsException(user.getId());
        }
    }
}
