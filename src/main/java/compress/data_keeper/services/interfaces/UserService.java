package compress.data_keeper.services.interfaces;

import compress.data_keeper.domain.entity.User;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;

public interface UserService {
    UserDto createUser(UserRegistrationDto userRegistrationDto);

    User getUserByEmail(String email);

    User saveUser(User user);
}
