package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.CouponBatch;
import com.backend.pojo.entity.CouponBatchStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponBatchMapper extends BaseMapper<CouponBatch> {
    List<CouponBatch> selectBatchPage(
            @Param("keyword") String keyword,
            @Param("status") CouponBatchStatus status,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countBatches(
            @Param("keyword") String keyword,
            @Param("status") CouponBatchStatus status
    );

    int incrementClaimedCount(@Param("id") Long id);
}
