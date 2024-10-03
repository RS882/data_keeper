package compress.data_keeper.security.services.interfaces;

import compress.data_keeper.security.domain.dto.LoginDto;
import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.security.domain.dto.TokensDto;
import compress.data_keeper.security.domain.dto.ValidationResponseDto;

public interface AuthService {
    TokensDto login(LoginDto loginDto);
    TokensDto refresh(String refreshToken);
    ValidationResponseDto validation( String authorizationHeader);
    void logout( String refreshToken);
    TokenResponseDto getTokenResponseDto(TokensDto tokensDto);

}
