package com.backend.sever.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SentinelProtectionConfig {
    private final SentinelProtectionProperties properties;

    public SentinelProtectionConfig(SentinelProtectionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initRules() {
        if (!properties.isEnabled()) {
            FlowRuleManager.loadRules(List.of());
            ParamFlowRuleManager.loadRules(List.of());
            DegradeRuleManager.loadRules(List.of());
            return;
        }
        FlowRuleManager.loadRules(List.of(
                flowRule(SentinelResourceNames.COUPON_CLAIM, properties.getCouponClaimQps()),
                flowRule(SentinelResourceNames.ACTIVITY_REGISTER, properties.getActivityRegisterQps()),
                flowRule(SentinelResourceNames.EMAIL_PASSWORD_RESET_CODE, properties.getEmailCodeQps())
        ));
        ParamFlowRuleManager.loadRules(List.of(
                hotParamRule(SentinelResourceNames.COUPON_CLAIM, properties.getCouponClaimHotBatchQps()),
                hotParamRule(SentinelResourceNames.ACTIVITY_REGISTER, properties.getActivityRegisterHotActivityQps()),
                hotParamRule(SentinelResourceNames.EMAIL_PASSWORD_RESET_CODE, properties.getEmailCodeHotEmailQps())
        ));
        DegradeRuleManager.loadRules(List.of(
                degradeRule(SentinelResourceNames.COUPON_CLAIM),
                degradeRule(SentinelResourceNames.ACTIVITY_REGISTER),
                degradeRule(SentinelResourceNames.EMAIL_PASSWORD_RESET_CODE)
        ));
    }

    private FlowRule flowRule(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(Math.max(qps, 1));
        return rule;
    }

    private ParamFlowRule hotParamRule(String resource, double qps) {
        ParamFlowRule rule = new ParamFlowRule(resource);
        rule.setParamIdx(0);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(Math.max(qps, 1));
        return rule;
    }

    private DegradeRule degradeRule(String resource) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(Math.min(Math.max(properties.getExceptionRatio(), 0.01), 1));
        rule.setMinRequestAmount(Math.max(properties.getMinRequestAmount(), 1));
        rule.setStatIntervalMs(Math.max(properties.getStatIntervalMs(), 1000));
        rule.setTimeWindow(Math.max(properties.getTimeWindowSeconds(), 1));
        return rule;
    }
}
