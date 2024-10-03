package compress.data_keeper.security.services;


import compress.data_keeper.domain.User;
import compress.data_keeper.repository.UserRepository;
import compress.data_keeper.security.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username)  {

        User user = repository.findByEmailAndIsActiveTrue(username)
                .orElseThrow(()-> new UsernameNotFoundException(
                        String.format("User with email %s not found", username)));

        return new AuthenticatedUser(user);
    }
}
