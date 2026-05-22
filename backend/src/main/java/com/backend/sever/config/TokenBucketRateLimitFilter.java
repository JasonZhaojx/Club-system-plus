package com.backend.sever.config;

import com.backend.sever.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TokenBucketRateLimitFilter extends OncePerRequestFilter {
    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])
            local current = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(current[1])
            local ts = tonumber(current[2])
            if tokens == nil then
                tokens = capacity
                ts = now
            end
            local delta = math.max(0, now - ts)
            local filled = math.min(capacity, tokens + delta * refill)
            local allowed = 0
            if filled >= requested then
                allowed = 1
                filled = filled - requested
            end
            redis.call('HMSET', key, 'tokens', filled, 'ts', now)
            redis.call('EXPIRE', key, ttl)
            return allowed
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    public TokenBucketRateLimitFilter(
            StringRedisTemplate redisTemplate,
            RateLimitProperties properties,
            ObjectMapper objectMapper,
            ClientIpResolver clientIpResolver
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.isEnabled() || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (allowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(429, "Too many requests")
        ));
    }

    private boolean allowed(HttpServletRequest request) {
        try {
            long now = System.currentTimeMillis() / 1000;
            String key = "rate:bucket:" + clientIpResolver.resolve(request) + ":" + request.getMethod() + ":" + normalizePath(request.getRequestURI());
            Long allowed = redisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(key),
                    String.valueOf(Math.max(properties.getCapacity(), 1)),
                    String.valueOf(Math.max(properties.getRefillPerSecond(), 1)),
                    String.valueOf(now),
                    "1",
                    "120"
            );
            return allowed == null || allowed == 1L;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/swagger-ui")
                || uri.contains("/v3/api-docs")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private String normalizePath(String uri) {
        return uri.replaceAll("/\\d+", "/{id}");
    }

}
