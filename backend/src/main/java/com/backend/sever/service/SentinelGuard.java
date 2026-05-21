package com.backend.sever.service;

public interface SentinelGuard {
    GuardEntry enter(String resource, Object hotParam);

    interface GuardEntry extends AutoCloseable {
        void trace(Throwable throwable);

        @Override
        void close();
    }
}
