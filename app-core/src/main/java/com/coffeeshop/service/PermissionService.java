package com.coffeeshop.service;

import com.coffeeshop.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Effective permission codes of a user (union over its active roles), cached per user like
 * MES PermissionService. Any change to roles / user-roles calls {@link #invalidateAll()}.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final Map<Long, Set<String>> cache = new ConcurrentHashMap<>();

    public Set<String> permissionsOf(Long userId) {
        return cache.computeIfAbsent(userId, id -> Set.copyOf(new HashSet<>(rolePermissionRepository.permissionCodesOfUser(id))));
    }

    public void invalidateAll() {
        cache.clear();
    }
}
