package com.backend.common.auth;

import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import com.backend.sever.mapper.UserMapper;
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
    private final AuthorizationCache authorizationCache;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthorizationCache authorizationCache,
            UserMapper userMapper
    ) {
        this.jwtService = jwtService;
        this.authorizationCache = authorizationCache;
        this.userMapper = userMapper;
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
                User user = userMapper.selectById(tokenPrincipal.userId());
                if (user == null || user.getStatus() != UserStatus.NORMAL) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
                int currentTokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                int tokenVersion = tokenPrincipal.tokenVersion() == null ? 0 : tokenPrincipal.tokenVersion();
                if (currentTokenVersion != tokenVersion) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
                AuthorizationCache.AuthorizationSnapshot authorization = authorizationCache.get(tokenPrincipal.userId());
                List<String> roles = authorization.roles();
                List<String> permissions = authorization.permissions();
                UserPrincipal principal = new UserPrincipal(
                        tokenPrincipal.userId(),
                        user.getUsername(),
                        currentTokenVersion,
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
