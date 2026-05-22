package com.backend.common.auth;

import java.security.Principal;
import java.util.List;

public record UserPrincipal(Long userId, String username, Integer tokenVersion, List<String> roles, List<String> permissions)
        implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
