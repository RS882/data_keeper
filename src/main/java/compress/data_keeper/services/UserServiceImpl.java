package compress.data_keeper.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.exception_handler.bad_requeat.exceptions.UserIdIsNullException;
import compress.data_keeper.exception_handler.not_found.exceptions.UserNotFoundByIdException;
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

    @Override
    public void checkUserById(Long id) {
        if (id == null) {
            throw new UserIdIsNullException();
        }
       if( !userRepository.existsById(id)) {
           throw new UserNotFoundByIdException(id);
       }
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->new UserNotFoundByIdException(id));
    }
}
