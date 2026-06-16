package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.Restaurant;
import com.backend.pojo.vo.RestaurantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface RestaurantMapper extends BaseMapper<Restaurant> {
    List<RestaurantVO> selectNearbyCandidates(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("category") String category
    );

    RestaurantVO selectDetail(@Param("id") Long id);
}
