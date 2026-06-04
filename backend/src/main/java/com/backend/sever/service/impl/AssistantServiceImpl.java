package com.backend.sever.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssistantChatRequestDTO;
import com.backend.pojo.entity.AssistantFaq;
import com.backend.pojo.vo.ActivityVO;
import com.backend.pojo.vo.AssistantChatResponseVO;
import com.backend.pojo.vo.AssistantSourceVO;
import com.backend.pojo.vo.CouponBatchVO;
import com.backend.pojo.vo.DepartmentVO;
import com.backend.pojo.vo.PageVO;
import com.backend.pojo.vo.UserCouponVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.AssistantFaqMapper;
import com.backend.sever.service.ActivityService;
import com.backend.sever.service.AiChatClient;
import com.backend.sever.service.AssistantMemoryService;
import com.backend.sever.service.AssistantMemoryService.MemoryMessage;
import com.backend.sever.service.AssistantService;
import com.backend.sever.service.AssistantUsageLimitService;
import com.backend.sever.service.CouponService;
import com.backend.sever.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
public class AssistantServiceImpl implements AssistantService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SYSTEM_PROMPT = """
            你是 Club System Plus 的社团智能助手。
            你只能根据【已检索到的系统数据】回答活动、优惠券、部门和 FAQ 问题。
            如果系统数据不足，请明确说当前没有查到，不要编造活动、库存、时间、地点、成员或政策。
            回答要简洁自然，可以使用中文英文；可以给出下一步操作建议，但不要声称已经替用户报名、领券或修改数据。
            """;

    private final ActivityService activityService;
    private final CouponService couponService;
    private final OrganizationService organizationService;
    private final AssistantFaqMapper assistantFaqMapper;
    private final AiChatClient aiChatClient;
    private final AssistantMemoryService assistantMemoryService;
    private final AssistantUsageLimitService assistantUsageLimitService;

    public AssistantServiceImpl(
            ActivityService activityService,
            CouponService couponService,
            OrganizationService organizationService,
            AssistantFaqMapper assistantFaqMapper,
            AiChatClient aiChatClient,
            AssistantMemoryService assistantMemoryService,
            AssistantUsageLimitService assistantUsageLimitService
    ) {
        this.activityService = activityService;
        this.couponService = couponService;
        this.organizationService = organizationService;
        this.assistantFaqMapper = assistantFaqMapper;
        this.aiChatClient = aiChatClient;
        this.assistantMemoryService = assistantMemoryService;
        this.assistantUsageLimitService = assistantUsageLimitService;
    }

    @Override
    public AssistantChatResponseVO chat(UserPrincipal principal, String clientIp, AssistantChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Message must not be blank");
        }

        String message = request.message().trim();
        assistantUsageLimitService.checkAndConsume(principal, clientIp);
        AssistantIntent intent = detectIntent(message);
        RetrievedContext context = retrieveContext(principal, message, intent);
        String memoryOwnerKey = memoryOwnerKey(principal, clientIp);
        String answer = aiChatClient.chat(
                SYSTEM_PROMPT,
                buildPrompt(message, intent, context.context(), assistantMemoryService.listRecentMessages(memoryOwnerKey))
        );
        assistantMemoryService.appendExchange(memoryOwnerKey, message, answer);
        return new AssistantChatResponseVO(answer, intent.name(), context.sources());
    }

    @Override
    public SseEmitter streamChat(UserPrincipal principal, String clientIp, AssistantChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Message must not be blank");
        }

        String message = request.message().trim();
        assistantUsageLimitService.checkAndConsume(principal, clientIp);
        AssistantIntent intent = detectIntent(message);
        RetrievedContext context = retrieveContext(principal, message, intent);
        String memoryOwnerKey = memoryOwnerKey(principal, clientIp);
        List<MemoryMessage> memory = assistantMemoryService.listRecentMessages(memoryOwnerKey);
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("meta")
                        .data(new AssistantChatResponseVO("", intent.name(), context.sources())));
                String answer = aiChatClient.streamChat(
                        SYSTEM_PROMPT,
                        buildPrompt(message, intent, context.context(), memory),
                        token -> sendToken(emitter, token)
                );
                assistantMemoryService.appendExchange(memoryOwnerKey, message, answer);
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(exception.getMessage()));
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (Exception ignored) {
                    // Ignore secondary SSE write failures.
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().name("token").data(new StreamToken(token)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send assistant stream token", exception);
        }
    }

    private RetrievedContext retrieveContext(UserPrincipal principal, String message, AssistantIntent intent) {
        return switch (intent) {
            case ACTIVITY_QUERY -> retrieveActivities(message);
            case COUPON_QUERY -> retrieveCoupons(principal, message);
            case DEPARTMENT_QUERY -> retrieveDepartments(message);
            case FAQ_QUERY -> retrieveFaq(message);
        };
    }

    private RetrievedContext retrieveActivities(String message) {
        String category = detectActivityCategory(message);
        PageVO<ActivityVO> page = activityService.listPublicActivities(null, category, "upcoming", 1, 6);
        List<AssistantSourceVO> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder("活动数据：\n");
        for (ActivityVO activity : page.getRecords()) {
            sources.add(new AssistantSourceVO("activity", String.valueOf(activity.getId()), activity.getTitle()));
            context.append("- ID: ").append(activity.getId())
                    .append("；标题: ").append(activity.getTitle())
                    .append("；分类: ").append(activity.getCategoryName())
                    .append("；摘要: ").append(activity.getSummary())
                    .append("；地点: ").append(activity.getLocation())
                    .append("；开始: ").append(formatDateTime(activity.getStartTime()))
                    .append("；结束: ").append(formatDateTime(activity.getEndTime()))
                    .append("；容量: ").append(activity.getCapacity())
                    .append("；已报名: ").append(activity.getRegisteredCount())
                    .append("；报名要求角色: ").append(blankToAll(activity.getRequiredRoleCode()))
                    .append("\n");
        }
        if (page.getRecords().isEmpty()) {
            context.append("当前没有查到匹配的公开活动。\n");
        }
        return new RetrievedContext(context.toString(), sources);
    }

    private RetrievedContext retrieveCoupons(UserPrincipal principal, String message) {
        List<AssistantSourceVO> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder("优惠券数据：\n");

        if (containsAny(message, "我的", "我有", "未使用", "已领取", "领过")) {
            if (principal == null) {
                return new RetrievedContext("用户未登录，无法查询个人优惠券。请先登录后再查询“我的优惠券”。\n", List.of());
            }
            List<UserCouponVO> coupons = couponService.listMyCoupons(principal).stream().limit(8).toList();
            for (UserCouponVO coupon : coupons) {
                sources.add(new AssistantSourceVO("user_coupon", String.valueOf(coupon.id()), coupon.batchName()));
                context.append("- 用户券ID: ").append(coupon.id())
                        .append("；名称: ").append(coupon.batchName())
                        .append("；权益: ").append(coupon.benefitText())
                        .append("；状态: ").append(coupon.status())
                        .append("；过期: ").append(formatDateTime(coupon.expireTime()))
                        .append("\n");
            }
            if (coupons.isEmpty()) {
                context.append("当前用户没有查到已领取优惠券。\n");
            }
            return new RetrievedContext(context.toString(), sources);
        }

        PageVO<CouponBatchVO> page = principal == null
                ? couponService.listBatches(null, com.backend.pojo.entity.CouponBatchStatus.ACTIVE, 1, 6)
                : couponService.listClaimableBatches(principal, null, 1, 6);
        for (CouponBatchVO batch : page.getRecords()) {
            sources.add(new AssistantSourceVO("coupon_batch", String.valueOf(batch.id()), batch.name()));
            context.append("- 批次ID: ").append(batch.id())
                    .append("；名称: ").append(batch.name())
                    .append("；说明: ").append(batch.description())
                    .append("；权益: ").append(batch.benefitText())
                    .append("；剩余: ").append(batch.remainingCount())
                    .append("；领取时间: ").append(formatDateTime(batch.claimStartTime()))
                    .append(" 至 ").append(formatDateTime(batch.claimEndTime()))
                    .append("；过期: ").append(formatDateTime(batch.expireTime()))
                    .append("；允许角色: ").append(batch.allowedRoleCodes())
                    .append("\n");
        }
        if (page.getRecords().isEmpty()) {
            context.append("当前没有查到可领取优惠券。\n");
        }
        return new RetrievedContext(context.toString(), sources);
    }

    private RetrievedContext retrieveDepartments(String message) {
        List<DepartmentVO> departments = organizationService.listDepartments();
        List<AssistantSourceVO> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder("部门数据：\n");
        for (DepartmentVO department : departments) {
            if (shouldIncludeDepartment(message, department)) {
                sources.add(new AssistantSourceVO("department", String.valueOf(department.getId()), department.getName()));
                context.append("- ID: ").append(department.getId())
                        .append("；名称: ").append(department.getName())
                        .append("；状态: ").append(department.getStatus())
                        .append("；介绍: ").append(department.getDescription())
                        .append("\n");
            }
        }
        if (sources.isEmpty()) {
            context.append("当前没有查到匹配的部门介绍。\n");
        }
        return new RetrievedContext(context.toString(), sources);
    }

    private RetrievedContext retrieveFaq(String message) {
        List<AssistantFaq> faqs = assistantFaqMapper.selectList(new LambdaQueryWrapper<AssistantFaq>()
                .eq(AssistantFaq::getEnabled, true)
                .and(wrapper -> wrapper
                        .like(AssistantFaq::getQuestion, message)
                        .or()
                        .like(AssistantFaq::getAnswer, message)
                        .or()
                        .like(AssistantFaq::getCategory, message))
                .last("limit 6"));

        if (faqs.isEmpty()) {
            faqs = assistantFaqMapper.selectList(new LambdaQueryWrapper<AssistantFaq>()
                    .eq(AssistantFaq::getEnabled, true)
                    .last("limit 6"));
        }

        List<AssistantSourceVO> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder("FAQ 数据：\n");
        for (AssistantFaq faq : faqs) {
            sources.add(new AssistantSourceVO("faq", String.valueOf(faq.getId()), faq.getQuestion()));
            context.append("- 问题: ").append(faq.getQuestion())
                    .append("；答案: ").append(faq.getAnswer())
                    .append("；分类: ").append(faq.getCategory())
                    .append("\n");
        }
        if (faqs.isEmpty()) {
            context.append("当前没有可用 FAQ。\n");
        }
        return new RetrievedContext(context.toString(), sources);
    }

    private String buildPrompt(String message, AssistantIntent intent, String context, List<MemoryMessage> memory) {
        return """
                最近对话记忆（最多 10 条）：
                %s

                用户问题：
                %s

                识别意图：
                %s

                已检索到的系统数据：
                %s

                请基于以上系统数据回答。若数据不足，请说明没有查到。
                """.formatted(formatMemory(memory), message, intent.name(), context);
    }

    private String formatMemory(List<MemoryMessage> memory) {
        if (memory == null || memory.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        for (MemoryMessage message : memory) {
            builder.append("- ")
                    .append(message.role())
                    .append(": ")
                    .append(message.content())
                    .append("\n");
        }
        return builder.toString();
    }

    private AssistantIntent detectIntent(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "优惠券", "优惠卷", "券", "领券", "抢券", "coupon")) {
            return AssistantIntent.COUPON_QUERY;
        }
        if (containsAny(normalized, "部门", "技术部", "运营部", "设计部", "外联部", "社团情况", "社团介绍", "department")) {
            return AssistantIntent.DEPARTMENT_QUERY;
        }
        if (containsAny(normalized, "活动", "报名", "地点", "时间", "讲座", "workshop", "hack", "event")) {
            return AssistantIntent.ACTIVITY_QUERY;
        }
        return AssistantIntent.FAQ_QUERY;
    }

    private String detectActivityCategory(String message) {
        if (containsAny(message, "技术", "ai", "AI", "workshop", "工作坊")) {
            return "technology";
        }
        if (containsAny(message, "竞赛", "挑战", "hack", "Hack")) {
            return "competition";
        }
        if (containsAny(message, "职业", "就业", "简历", "career")) {
            return "career";
        }
        if (containsAny(message, "社区", "开放日", "community")) {
            return "community";
        }
        return null;
    }

    private boolean shouldIncludeDepartment(String message, DepartmentVO department) {
        String name = department.getName() == null ? "" : department.getName();
        return !containsAny(message, "技术部", "运营部", "设计部", "外联部")
                || message.contains(name)
                || (message.contains("技术") && name.contains("技术"))
                || (message.contains("运营") && name.contains("运营"))
                || (message.contains("设计") && name.contains("设计"))
                || (message.contains("外联") && name.contains("外联"));
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "未设置" : DATE_TIME_FORMATTER.format(value);
    }

    private String blankToAll(String value) {
        return StringUtils.hasText(value) ? value : "不限";
    }

    private String memoryOwnerKey(UserPrincipal principal, String clientIp) {
        if (principal != null) {
            return "user:" + principal.userId();
        }
        return "guest:" + hash(StringUtils.hasText(clientIp) ? clientIp : "unknown");
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash assistant memory key", exception);
        }
    }

    private enum AssistantIntent {
        ACTIVITY_QUERY,
        COUPON_QUERY,
        DEPARTMENT_QUERY,
        FAQ_QUERY
    }

    private record RetrievedContext(String context, List<AssistantSourceVO> sources) {
    }

    private record StreamToken(String token) {
    }
}
