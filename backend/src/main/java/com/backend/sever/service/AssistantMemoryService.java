package com.backend.sever.service;

import java.util.List;

public interface AssistantMemoryService {
    List<MemoryMessage> listRecentMessages(String ownerKey);

    void appendExchange(String ownerKey, String userMessage, String assistantMessage);

    record MemoryMessage(String role, String content) {
    }
}
