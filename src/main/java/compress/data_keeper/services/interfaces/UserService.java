package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.dto.users.UserDto;

public interface UserService {
    UserDto createUser(UserDto userDto);
}
