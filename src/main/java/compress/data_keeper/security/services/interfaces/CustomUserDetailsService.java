package compress.data_keeper.security.services.interfaces;


import compress.data_keeper.domain.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface CustomUserDetailsService extends UserDetailsService {
     void updateUser(User user) ;
}
