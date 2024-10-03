package compress.data_keeper.security.services.mapping;

import compress.data_keeper.security.domain.dto.TokenResponseDto;
import compress.data_keeper.security.domain.dto.TokensDto;
import org.mapstruct.Mapper;

@Mapper
public abstract class TokenDtoMapperService {
   public abstract TokenResponseDto toResponseDto(TokensDto tokensDto) ;
}
