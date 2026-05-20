package com.backend.common.auth;

import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;

@Service
public class AuthorizationCache {
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final Cache<Long, AuthorizationSnapshot> cache;

    public AuthorizationCache(RoleMapper roleMapper, PermissionMapper permissionMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.cache = Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofMinutes(3))
                .build();
    }

    public AuthorizationSnapshot get(Long userId) {
        return cache.get(userId, this::load);
    }

    public void evictUser(Long userId) {
        if (userId != null) {
            afterCommit(() -> cache.invalidate(userId));
        }
    }

    public void evictAll() {
        afterCommit(cache::invalidateAll);
    }

    private AuthorizationSnapshot load(Long userId) {
        List<String> roles = roleMapper.selectByUserId(userId)
                .stream()
                .map(role -> role.getCode())
                .toList();
        List<String> permissions = permissionMapper.selectByUserId(userId)
                .stream()
                .map(permission -> permission.getCode())
                .toList();
        return new AuthorizationSnapshot(roles, permissions);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public record AuthorizationSnapshot(List<String> roles, List<String> permissions) {
    }
}
