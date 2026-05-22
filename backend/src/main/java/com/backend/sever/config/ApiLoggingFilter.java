package com.backend.sever.config;

import com.backend.common.auth.UserPrincipal;
import com.backend.sever.mapper.DashboardMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {
    private final DashboardMapper dashboardMapper;
    private final ClientIpResolver clientIpResolver;

    public ApiLoggingFilter(DashboardMapper dashboardMapper, ClientIpResolver clientIpResolver) {
        this.dashboardMapper = dashboardMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            UserPrincipal principal = currentPrincipal();
            String path = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            String ipAddress = clientIpResolver.resolve(request);
            try {
                dashboardMapper.insertApiAccessLog(
                        method,
                        path,
                        status,
                        durationMs,
                        principal == null ? null : principal.userId(),
                        principal == null ? null : principal.username(),
                        ipAddress,
                        request.getHeader("User-Agent")
                );
                if (isOperation(method, status)) {
                    dashboardMapper.insertOperationLog(
                            principal == null ? null : principal.userId(),
                            principal == null ? null : principal.username(),
                            method,
                            path,
                            resolveAction(method, path),
                            status,
                            durationMs,
                            ipAddress
                    );
                }
            } catch (RuntimeException ignored) {
                // Logging must never break business requests.
            }
        }
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private boolean isOperation(String method, int status) {
        return status < 400 && !"GET".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method);
    }

    private String resolveAction(String method, String path) {
        if (path.contains("/auth/login")) {
            return "用户登录";
        }
        if (path.contains("/auth/register")) {
            return "用户注册";
        }
        if (path.contains("/activities")) {
            return "活动变更";
        }
        if (path.contains("/coupons")) {
            return "优惠券变更";
        }
        if (path.contains("/organization")) {
            return "组织变更";
        }
        if (path.contains("/rbac")) {
            return "权限变更";
        }
        return method + " " + path;
    }

}
