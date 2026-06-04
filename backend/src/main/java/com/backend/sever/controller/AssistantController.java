package com.backend.sever.controller;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssistantChatRequestDTO;
import com.backend.pojo.vo.AssistantChatResponseVO;
import com.backend.sever.common.Result;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.AssistantService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/assistant")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public Result<AssistantChatResponseVO> chat(
            Authentication authentication,
            @RequestBody AssistantChatRequestDTO request
    ) {
        return Result.success(assistantService.chat(currentPrincipal(authentication), null, request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            Authentication authentication,
            @RequestBody AssistantChatRequestDTO request
    ) {
        return assistantService.streamChat(currentPrincipal(authentication), null, request);
    }

    private UserPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
