package vn.edu.iuh.english_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.english_practice.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    List<User> findAllByUserName(String userName);

    List<User> findByUserName(String userName);

    @Query("SELECT u FROM User u WHERE u.provierId = :providerId ")
    Optional<User> findByProviderId(@Param("providerId") String providerId);
}
