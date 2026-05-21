package com.backend.sever.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.backend.sever.config.SentinelProtectionProperties;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.service.SentinelGuard;
import org.springframework.stereotype.Service;

@Service
public class SentinelGuardImpl implements SentinelGuard {
    private final SentinelProtectionProperties properties;

    public SentinelGuardImpl(SentinelProtectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public GuardEntry enter(String resource, Object hotParam) {
        if (!properties.isEnabled()) {
            return NoopGuardEntry.INSTANCE;
        }
        try {
            Object[] args = hotParam == null ? new Object[0] : new Object[]{hotParam};
            return new SentinelGuardEntry(SphU.entry(resource, EntryType.IN, 1, args), args);
        } catch (BlockException exception) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "当前服务繁忙，请稍后再试");
        }
    }

    private record SentinelGuardEntry(Entry entry, Object[] args) implements GuardEntry {
        @Override
        public void trace(Throwable throwable) {
            if (throwable != null && !(throwable instanceof BusinessException)) {
                Tracer.traceEntry(throwable, entry);
            }
        }

        @Override
        public void close() {
            entry.exit(1, args);
        }
    }

    private enum NoopGuardEntry implements GuardEntry {
        INSTANCE;

        @Override
        public void trace(Throwable throwable) {
        }

        @Override
        public void close() {
        }
    }
}
