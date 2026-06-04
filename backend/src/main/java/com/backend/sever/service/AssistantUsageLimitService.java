package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;

public interface AssistantUsageLimitService {
    void checkAndConsume(UserPrincipal principal, String clientIp);
}
