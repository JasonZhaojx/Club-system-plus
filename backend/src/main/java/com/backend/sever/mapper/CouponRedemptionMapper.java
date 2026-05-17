package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.CouponRedemption;
import com.backend.pojo.vo.CouponRedemptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponRedemptionMapper extends BaseMapper<CouponRedemption> {
    List<CouponRedemptionVO> selectUserRedemptions(@Param("userId") Long userId);
}
