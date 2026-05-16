package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.Department;
import com.backend.pojo.entity.DepartmentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    int updateDepartment(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("description") String description
    );

    int updateStatus(@Param("id") Long id, @Param("status") DepartmentStatus status);
}
