package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.User;
import com.backend.pojo.entity.UserStatus;
import com.backend.pojo.vo.AdminUserVO;
import com.backend.pojo.vo.UserMembershipVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname, @Param("email") String email);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    UserMembershipVO selectMembershipByUserId(@Param("userId") Long userId);

    long countAdminUsers(@Param("keyword") String keyword, @Param("departmentId") Long departmentId);

    List<AdminUserVO> selectAdminUserPage(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("offset") int offset,
            @Param("size") int size
    );
}
