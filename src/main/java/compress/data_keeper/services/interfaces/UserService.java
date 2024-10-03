package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;

public interface UserService {
    UserDto createUser(UserRegistrationDto userRegistrationDto);
    void checkUserById(Long id);
    User getUserById(Long id);
}
