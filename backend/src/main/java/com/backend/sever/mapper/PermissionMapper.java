package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    List<Permission> selectByUserId(@Param("userId") Long userId);

    int deleteRolePermissions(@Param("roleCode") String roleCode);

    int insertRolePermissionByCode(
            @Param("roleCode") String roleCode,
            @Param("permissionCode") String permissionCode
    );
}
