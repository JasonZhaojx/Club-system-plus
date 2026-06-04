package com.backend.sever.service;

import java.util.function.Consumer;

public interface AiChatClient {
    String chat(String systemPrompt, String userPrompt);

    String streamChat(String systemPrompt, String userPrompt, Consumer<String> tokenConsumer);
}
