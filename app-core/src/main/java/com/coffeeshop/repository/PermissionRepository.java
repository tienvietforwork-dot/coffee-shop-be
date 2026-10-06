package com.coffeeshop.repository;

import com.coffeeshop.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findAllByOrderBySortOrderAscCodeAsc();

    List<Permission> findByCodeInOrderBySortOrderAsc(Collection<String> codes);
}
