package kz.nurbolat.bookaggregator.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Deletes the row; the database cascades to everything the user owns. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM User u WHERE u.id = :id")
    int deleteUserById(Long id);

    /** Locks the user's row until the transaction ends, so that sign-ins of one user run one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> lockById(Long id);
}
