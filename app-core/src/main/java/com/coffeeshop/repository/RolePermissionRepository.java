package com.coffeeshop.repository;

import com.coffeeshop.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    /** Effective permission codes of a user over all its active roles. */
    @Query("""
            select distinct p.code from UserRole ur, RolePermission rp
            join rp.permission p
            where rp.role = ur.role and ur.user.id = :userId
              and ur.role.active = true and p.active = true
            """)
    List<String> permissionCodesOfUser(Long userId);

    @Query("select rp from RolePermission rp join fetch rp.permission where rp.role.id = :roleId")
    List<RolePermission> findByRoleId(Long roleId);
}
