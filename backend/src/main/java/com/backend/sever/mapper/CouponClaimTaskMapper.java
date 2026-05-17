package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.CouponClaimTask;
import com.backend.pojo.entity.CouponClaimTaskStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponClaimTaskMapper extends BaseMapper<CouponClaimTask> {
    CouponClaimTask selectByBatchAndUser(@Param("batchId") Long batchId, @Param("userId") Long userId);

    List<CouponClaimTask> selectRetryableTasks(@Param("size") int size);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") CouponClaimTaskStatus status,
            @Param("errorMessage") String errorMessage
    );
}
