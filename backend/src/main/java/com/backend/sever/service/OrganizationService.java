package com.backend.sever.service;

import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssignDepartmentLeaderDTO;
import com.backend.pojo.dto.AssignMemberDepartmentDTO;
import com.backend.pojo.dto.CreateDepartmentDTO;
import com.backend.pojo.dto.UpdateDepartmentDTO;
import com.backend.pojo.dto.UpdateMemberStatusDTO;
import com.backend.pojo.vo.ClubMemberVO;
import com.backend.pojo.vo.DepartmentLeaderVO;
import com.backend.pojo.vo.DepartmentVO;
import com.backend.pojo.vo.AdminUserVO;
import com.backend.pojo.vo.PageVO;

import java.util.List;

public interface OrganizationService {
    List<DepartmentVO> listDepartments();

    DepartmentVO createDepartment(CreateDepartmentDTO request);

    DepartmentVO updateDepartment(Long departmentId, UpdateDepartmentDTO request);

    void disableDepartment(Long departmentId);

    void enableDepartment(Long departmentId);

    List<ClubMemberVO> listMembers(UserPrincipal principal, Long departmentId);

    PageVO<AdminUserVO> listUsers(String keyword, Long departmentId, int page, int size);

    ClubMemberVO assignMemberToDepartment(UserPrincipal principal, AssignMemberDepartmentDTO request);

    ClubMemberVO updateMemberStatus(UserPrincipal principal, UpdateMemberStatusDTO request);

    List<DepartmentLeaderVO> listLeaders(UserPrincipal principal, Long departmentId);

    void appointDepartmentLeader(AssignDepartmentLeaderDTO request);

    void removeDepartmentLeader(AssignDepartmentLeaderDTO request);
}
