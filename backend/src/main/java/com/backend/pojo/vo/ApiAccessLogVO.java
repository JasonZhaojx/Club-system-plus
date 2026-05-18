package com.backend.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiAccessLogVO {
    private Long id;
    private String method;
    private String path;
    private Integer statusCode;
    private Long durationMs;
    private Long userId;
    private String username;
    private String ipAddress;
    private LocalDateTime createdAt;
}
