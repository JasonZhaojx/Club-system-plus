package com.backend.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String method;
    private String path;
    private String action;
    private Integer statusCode;
    private Long durationMs;
    private String ipAddress;
    private LocalDateTime createdAt;
}
