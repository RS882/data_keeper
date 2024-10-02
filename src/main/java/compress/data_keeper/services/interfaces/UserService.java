package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.users.UserDto;

public interface UserService {
    UserDto createUser(UserDto userDto);
    void checkUserById(Long id);
    User getUserById(Long id);
}
