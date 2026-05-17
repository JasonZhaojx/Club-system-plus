package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.UserCoupon;
import com.backend.pojo.entity.UserCouponStatus;
import com.backend.pojo.vo.UserCouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    UserCoupon selectByBatchAndUser(@Param("batchId") Long batchId, @Param("userId") Long userId);

    List<UserCouponVO> selectUserCoupons(@Param("userId") Long userId);

    UserCouponVO selectUserCouponDetail(@Param("id") Long id, @Param("userId") Long userId);

    int updateStatus(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") UserCouponStatus status
    );
}
