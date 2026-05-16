package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);
}
