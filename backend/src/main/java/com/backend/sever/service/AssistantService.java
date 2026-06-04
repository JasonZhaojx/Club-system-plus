package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssistantChatRequestDTO;
import com.backend.pojo.vo.AssistantChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AssistantService {
    AssistantChatResponseVO chat(UserPrincipal principal, String clientIp, AssistantChatRequestDTO request);

    SseEmitter streamChat(UserPrincipal principal, String clientIp, AssistantChatRequestDTO request);
}
