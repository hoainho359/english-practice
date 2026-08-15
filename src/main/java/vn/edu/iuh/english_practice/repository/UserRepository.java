package vn.edu.iuh.english_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.english_practice.entity.User;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Collection<Object> findByUserName(String userName);

    List<User> findAllByUserName(String userName);
}
