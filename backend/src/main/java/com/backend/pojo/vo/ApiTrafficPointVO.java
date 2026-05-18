package com.backend.pojo.vo;

import lombok.Data;

@Data
public class ApiTrafficPointVO {
    private String bucket;
    private long total;
    private long errorCount;
    private Long avgDurationMs;
}
