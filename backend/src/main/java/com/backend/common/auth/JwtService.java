package com.backend.common.auth;

import com.backend.pojo.entity.User;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generate(User user) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "sub", String.valueOf(user.getId()),
                "username", user.getUsername(),
                "iat", now,
                "exp", now + properties.expirationSeconds()
        );
        String unsignedToken = base64Json(header) + "." + base64Json(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public UserPrincipal parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Map<?, ?> payload = objectMapper.readValue(base64UrlDecode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            if (exp == null || exp.longValue() < Instant.now().getEpochSecond()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
            String username = String.valueOf(payload.get("username"));
            return new UserPrincipal(userId, username, List.of(), List.of());
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String base64Json(Object value) {
        try {
            return base64UrlEncode(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to build JWT", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
