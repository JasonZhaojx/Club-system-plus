package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.DepartmentLeader;
import com.backend.pojo.vo.DepartmentLeaderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentLeaderMapper extends BaseMapper<DepartmentLeader> {
    List<Long> selectDepartmentIdsByUserId(@Param("userId") Long userId);

    List<DepartmentLeaderVO> selectLeaderVOList(@Param("departmentId") Long departmentId);

    int insertLeader(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    int deleteLeader(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    int deleteByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);
}
