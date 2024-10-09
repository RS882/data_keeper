package compress.data_keeper.services.mapping;

import compress.data_keeper.domain.entity.User;

import compress.data_keeper.domain.dto.users.UserDto;
import compress.data_keeper.domain.dto.users.UserRegistrationDto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper
public abstract class UserMapperService {

    @Autowired
    protected PasswordEncoder encoder;

    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "name", source = "userName")
    @Mapping(target = "role", expression = "java(compress.data_keeper.security.contstants.Role.ROLE_USER)")
    @Mapping(target="password", expression = "java(encoder.encode(dto.getPassword()))")
    @Mapping(target = "loginBlockedUntil", expression = "java(java.time.LocalDateTime.now())"  )
    public abstract User toEntity(UserRegistrationDto dto);

     @Mapping(target = "userName", source = "name")
     @Mapping(target = "userId", source = "id")
    public abstract UserDto toDto(User user);
}
