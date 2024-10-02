package compress.data_keeper.controllers;

import compress.data_keeper.controllers.API.UserAPI;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserAPI {

    private final UserService userService;

    @Override
    public ResponseEntity<UserDto> createUser(UserDto userDto) {
        return ResponseEntity.ok(userService.createUser(userDto));
    }
}
