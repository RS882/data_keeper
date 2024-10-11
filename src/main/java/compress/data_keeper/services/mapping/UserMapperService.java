package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;
import compress.data_keeper.domain.entity.User;
import compress.data_keeper.security.contstants.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static compress.data_keeper.security.contstants.Role.ROLE_USER;

@Mapper
public abstract class UserMapperService {

    @Autowired
    protected PasswordEncoder encoder;

    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "name", source = "userName")
    @Mapping(target = "role", expression = "java(getDefaultRole())")
    @Mapping(target = "password", expression = "java(encodePassword(dto))")
    @Mapping(target = "loginBlockedUntil", expression = "java(getDefaultLoginBlockedUntil())")
    public abstract User toEntity(UserRegistrationDto dto);

    @Mapping(target = "userName", source = "name")
    @Mapping(target = "userId", source = "id")
    public abstract UserDto toDto(User user);

    protected Role getDefaultRole() {
        return ROLE_USER;
    }

    protected String encodePassword(UserRegistrationDto dto) {
        return encoder.encode(dto.getPassword());
    }

    protected LocalDateTime getDefaultLoginBlockedUntil() {
        return LocalDateTime.now();
    }
}
