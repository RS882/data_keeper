package compress.data_keeper.security.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.exception_handler.not_found.exceptions.TokenNotFoundException;
import compress.data_keeper.security.domain.dto.TokensDto;
import compress.data_keeper.security.domain.entity.RefreshToken;
import compress.data_keeper.security.repositorys.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private final SecretKey ACCESS_KEY;
    private final SecretKey REFRESH_KEY;

    public static final int ACCESS_TOKEN_EXPIRES_IN_MINUTES = 30;
    public static final int REFRESH_TOKEN_EXPIRES_IN_MINUTES = 15 * 24 * 60;
    public static final String USER_ROLE_VARIABLE_NAME = "role";
    public static final String USER_EMAIL_VARIABLE_NAME = "email";

    private static final String TOKENS_ISSUER = "Authorization";

    private final TokenRepository repository;

    private Date refreshTokenExpireAt;

    public TokenService(@Value("${key.access}") String accessKey,
                        @Value("${key.refresh}") String refreshKey,
                        TokenRepository repository) {
        this.ACCESS_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessKey));
        this.REFRESH_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshKey));
        this.repository = repository;
    }

    public TokensDto getTokens(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        saveRefreshToken(refreshToken, user);

        return TokensDto.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public List<String> getRefreshTokensByUserId(Long id) {
        List<RefreshToken> refreshTokens = repository.findByUserId(id).orElseThrow(
                () -> new TokenNotFoundException("Token not found"));
        return refreshTokens.stream()
                .map(RefreshToken::getToken)
                .collect(Collectors.toList());
    }

    public boolean validateRefreshToken(String refreshToken) {
        return isTokenValid(refreshToken, REFRESH_KEY);
    }

    public boolean validateAccessToken(String accessToken) {
        return isTokenValid(accessToken, ACCESS_KEY);
    }

    public Claims getRefreshTokenClaims(String refreshToken) {
        return getClaims(refreshToken, REFRESH_KEY);
    }

    public Claims getAccessTokenClaims(String accessToken) {
        return getClaims(accessToken, ACCESS_KEY);
    }

    @Transactional
    public void removeOldRefreshToken(String oldRefreshToken) {
        repository.deleteAllByToken(oldRefreshToken);
    }

    private String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .expiration(getExpirationDate(ACCESS_TOKEN_EXPIRES_IN_MINUTES))
                .issuer(TOKENS_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .signWith(ACCESS_KEY)
                .claim(USER_ROLE_VARIABLE_NAME, user.getRole())
                .claim(USER_EMAIL_VARIABLE_NAME, user.getEmail())
                .compact();
    }

    private String generateRefreshToken(User user) {
        this.refreshTokenExpireAt = getExpirationDate(REFRESH_TOKEN_EXPIRES_IN_MINUTES);
        return Jwts.builder()
                .subject(user.getEmail())
                .expiration(this.refreshTokenExpireAt)
                .issuer(TOKENS_ISSUER)
                .issuedAt(Date.from(Instant.now()))
                .signWith(REFRESH_KEY)
                .compact();
    }

    private void saveRefreshToken(String refreshToken, User user) {
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expireAt(
                        Instant.ofEpochMilli(this.refreshTokenExpireAt.getTime())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                )
                .build();
        repository.save(refreshTokenEntity);
    }

    private Date getExpirationDate(int expiresInMinutes) {
        return Date.from(LocalDateTime.now()
                .plusMinutes(expiresInMinutes)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    private boolean isTokenValid(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
