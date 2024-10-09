package compress.data_keeper.services;

import compress.data_keeper.domain.User;
import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.exception_handler.unauthorized.UnauthorizedException;
import compress.data_keeper.exception_handler.bad_requeat.BadRequestException;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.services.interfaces.UserService;
import compress.data_keeper.services.mapping.UserMapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapperService mapper;

    @Override
    public UserDto createUser(UserRegistrationDto userRegistrationDto) {

        if (userRepository.existsByEmail(userRegistrationDto.getEmail()))
            throw new BadRequestException("Email address already in use");

        User savedUser = saveUser(mapper.toEntity(userRegistrationDto));

        return mapper.toDto(savedUser);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(UnauthorizedException::new);
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
