package com.coffeeshop.repository;

import com.coffeeshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("select distinct u from User u left join fetch u.userRoles ur left join fetch ur.role "
            + "left join fetch u.staff left join fetch u.customer where u.username = :username")
    Optional<User> findWithRolesByUsername(String username);

    @Query("select distinct u from User u left join fetch u.userRoles ur left join fetch ur.role "
            + "left join fetch u.staff left join fetch u.customer order by u.username")
    List<User> findAllWithRoles();

    boolean existsByUsername(String username);

    boolean existsByStaffIdAndIdNot(Long staffId, Long id);

    boolean existsByCustomerIdAndIdNot(Long customerId, Long id);

    boolean existsByCustomerId(Long customerId);

    /** Bulk update so logging in does not touch updated_by / updated_at. */
    @Modifying
    @Query("update User u set u.lastLoginAt = :at where u.id = :id")
    void touchLastLogin(Long id, LocalDateTime at);
}
