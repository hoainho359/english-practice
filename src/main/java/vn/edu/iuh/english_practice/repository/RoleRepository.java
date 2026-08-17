package vn.edu.iuh.english_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.english_practice.entity.Permission;
import vn.edu.iuh.english_practice.entity.Role;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    @Query("""
           select r from  Role  r 
                      join  r.users u
                                 where u.userName = :userName 
                      """)
    List<Role> findAllByUserName(@Param("userName") String userName);

    List<Role> findAllByNameIn(Collection<String> names);

    List<Role> findByName(String name);
}
