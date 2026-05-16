package com.backend.common.auth;

import com.backend.sever.exception.BusinessException;
import com.backend.sever.mapper.PermissionMapper;
import com.backend.sever.mapper.RoleMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper
    ) {
        this.jwtService = jwtService;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                UserPrincipal tokenPrincipal = jwtService.parse(header.substring(7));
                List<String> roles = roleMapper.selectByUserId(tokenPrincipal.userId())
                        .stream()
                        .map(role -> role.getCode())
                        .toList();
                List<String> permissions = permissionMapper.selectByUserId(tokenPrincipal.userId())
                        .stream()
                        .map(permission -> permission.getCode())
                        .toList();
                UserPrincipal principal = new UserPrincipal(
                        tokenPrincipal.userId(),
                        tokenPrincipal.username(),
                        roles,
                        permissions
                );
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                principal,
                                null,
                                Stream.concat(
                                                roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                                                permissions.stream().map(SimpleGrantedAuthority::new)
                                        )
                                        .toList()
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
