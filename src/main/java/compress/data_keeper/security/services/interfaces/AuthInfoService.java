package compress.data_keeper.security.services.interfaces;

import compress.data_keeper.security.domain.AuthInfo;
import io.jsonwebtoken.Claims;

public interface AuthInfoService {

    AuthInfo mapClaims(Claims claims);
}
