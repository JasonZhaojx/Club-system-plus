package com.backend.sever.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.backend.pojo.entity.ClubMember;
import com.backend.pojo.entity.MemberStatus;
import com.backend.pojo.vo.ClubMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClubMemberMapper extends BaseMapper<ClubMember> {
    ClubMember selectByUserId(@Param("userId") Long userId);

    ClubMemberVO selectMemberVOByUserId(@Param("userId") Long userId);

    List<ClubMemberVO> selectMemberVOList(@Param("departmentId") Long departmentId);

    int upsertMember(
            @Param("userId") Long userId,
            @Param("departmentId") Long departmentId,
            @Param("status") MemberStatus status
    );

    int updateStatus(@Param("userId") Long userId, @Param("status") MemberStatus status);
}
