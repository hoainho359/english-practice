package vn.edu.iuh.english_practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.english_practice.entity.Permission;
import vn.edu.iuh.english_practice.entity.User;

import java.util.Collection;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {


    @Query("""
            select p from Permission  p
                    join p.roles r
                            join  r.users u
                                  where u.userName = :userName
            
            """)
    List<Permission> findPermissionsByUsername(@Param("userName") String username);

    List<Permission> findAllByNameIn(Collection<String> names);
}
