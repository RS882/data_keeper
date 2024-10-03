package compress.data_keeper.security.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.security.domain.AuthInfo;
import compress.data_keeper.security.services.interfaces.AuthInfoService;
import compress.data_keeper.services.interfaces.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthInfoServiceImpl implements AuthInfoService {

    private final UserService userService;

    @Override
    public AuthInfo mapClaims(Claims claims) {

        String userEmail = claims.getSubject();

        User currentUser = userService.getUserByEmail(userEmail);
        return new AuthInfo(currentUser);
    }
}
