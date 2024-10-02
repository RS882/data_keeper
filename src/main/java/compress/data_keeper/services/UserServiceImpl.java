package compress.data_keeper.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto createUser(UserDto dto) {
        User user = User.builder()
                .userName(dto.getUserName())
                .build();

        User savedUser = userRepository.save(user);

        return UserDto.from(savedUser);
    }
}
