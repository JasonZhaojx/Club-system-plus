package com.backend.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardConversionVO {
    private String name;
    private long current;
    private long target;
    private double rate;
}
