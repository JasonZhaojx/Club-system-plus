package com.backend.common.auth;

import java.security.Principal;

public record UserPrincipal(Long userId, String username) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
