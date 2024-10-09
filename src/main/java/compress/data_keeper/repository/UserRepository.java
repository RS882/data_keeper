package compress.data_keeper.repository;

import compress.data_keeper.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsActiveTrue(String email);

    void deleteAllByEmail(String email);

    boolean existsByEmail(String email);
}
