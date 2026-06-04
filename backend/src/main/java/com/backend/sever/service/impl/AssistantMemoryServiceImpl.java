package com.backend.sever.service.impl;

import com.backend.sever.service.AssistantMemoryService;
import com.backend.sever.service.AssistantMemoryService.MemoryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AssistantMemoryServiceImpl implements AssistantMemoryService {
    private static final String KEY_PREFIX = "assistant:memory:";
    private static final int MAX_MESSAGES = 10;
    private static final Duration MEMORY_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AssistantMemoryServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MemoryMessage> listRecentMessages(String ownerKey) {
        if (!StringUtils.hasText(ownerKey)) {
            return List.of();
        }
        try {
            List<String> rawMessages = redisTemplate.opsForList().range(key(ownerKey), 0, MAX_MESSAGES - 1);
            if (rawMessages == null || rawMessages.isEmpty()) {
                return List.of();
            }
            List<MemoryMessage> messages = new ArrayList<>();
            for (String rawMessage : rawMessages) {
                MemoryMessage message = objectMapper.readValue(rawMessage, MemoryMessage.class);
                if (StringUtils.hasText(message.role()) && StringUtils.hasText(message.content())) {
                    messages.add(message);
                }
            }
            Collections.reverse(messages);
            return messages;
        } catch (RuntimeException exception) {
            return List.of();
        } catch (Exception exception) {
            return List.of();
        }
    }

    @Override
    public void appendExchange(String ownerKey, String userMessage, String assistantMessage) {
        if (!StringUtils.hasText(ownerKey) || !StringUtils.hasText(userMessage) || !StringUtils.hasText(assistantMessage)) {
            return;
        }
        try {
            String key = key(ownerKey);
            redisTemplate.opsForList().leftPush(key, objectMapper.writeValueAsString(new MemoryMessage("user", userMessage)));
            redisTemplate.opsForList().leftPush(key, objectMapper.writeValueAsString(new MemoryMessage("assistant", assistantMessage)));
            redisTemplate.opsForList().trim(key, 0, MAX_MESSAGES - 1);
            redisTemplate.expire(key, MEMORY_TTL);
        } catch (Exception ignored) {
            // Memory is helpful context, not a critical business dependency.
        }
    }

    private String key(String ownerKey) {
        return KEY_PREFIX + ownerKey;
    }
}
