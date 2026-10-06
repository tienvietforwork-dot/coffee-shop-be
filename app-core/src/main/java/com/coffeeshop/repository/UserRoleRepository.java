package com.coffeeshop.repository;

import com.coffeeshop.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    @Query("select count(distinct ur.user.id) from UserRole ur where ur.role.id = :roleId")
    long countUsers(Long roleId);
}
