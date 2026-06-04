package com.backend.pojo.vo;

import java.util.List;

public record AssistantChatResponseVO(
        String answer,
        String intent,
        List<AssistantSourceVO> sources
) {
}
