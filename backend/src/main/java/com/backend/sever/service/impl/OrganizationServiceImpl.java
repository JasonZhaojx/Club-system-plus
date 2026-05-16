package com.backend.sever.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.backend.common.auth.UserPrincipal;
import com.backend.pojo.dto.AssignDepartmentLeaderDTO;
import com.backend.pojo.dto.AssignMemberDepartmentDTO;
import com.backend.pojo.dto.CreateDepartmentDTO;
import com.backend.pojo.dto.UpdateDepartmentDTO;
import com.backend.pojo.dto.UpdateMemberStatusDTO;
import com.backend.pojo.entity.Department;
import com.backend.pojo.entity.DepartmentStatus;
import com.backend.pojo.entity.MemberStatus;
import com.backend.pojo.entity.User;
import com.backend.pojo.vo.ClubMemberVO;
import com.backend.pojo.vo.DepartmentLeaderVO;
import com.backend.pojo.vo.DepartmentVO;
import com.backend.pojo.vo.AdminUserVO;
import com.backend.pojo.vo.PageVO;
import com.backend.sever.exception.BusinessException;
import com.backend.sever.exception.ErrorCode;
import com.backend.sever.mapper.ClubMemberMapper;
import com.backend.sever.mapper.DepartmentLeaderMapper;
import com.backend.sever.mapper.DepartmentMapper;
import com.backend.sever.mapper.RoleMapper;
import com.backend.sever.mapper.UserMapper;
import com.backend.sever.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrganizationServiceImpl implements OrganizationService {
    private static final String CLUB_MEMBER_ROLE = "CLUB_MEMBER";
    private static final String REGISTERED_USER_ROLE = "REGISTERED_USER";
    private static final String DEPARTMENT_LEADER_ROLE = "DEPARTMENT_LEADER";

    private final DepartmentMapper departmentMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final DepartmentLeaderMapper departmentLeaderMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public OrganizationServiceImpl(
            DepartmentMapper departmentMapper,
            ClubMemberMapper clubMemberMapper,
            DepartmentLeaderMapper departmentLeaderMapper,
            UserMapper userMapper,
            RoleMapper roleMapper
    ) {
        this.departmentMapper = departmentMapper;
        this.clubMemberMapper = clubMemberMapper;
        this.departmentLeaderMapper = departmentLeaderMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public List<DepartmentVO> listDepartments() {
        return departmentMapper.selectList(
                        new LambdaQueryWrapper<Department>().orderByAsc(Department::getId)
                )
                .stream()
                .map(DepartmentVO::from)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentVO createDepartment(CreateDepartmentDTO request) {
        String name = requireDepartmentName(request == null ? null : request.getName());
        if (departmentMapper.countByName(name, null) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门名称已存在");
        }
        Department department = new Department();
        department.setName(name);
        department.setDescription(normalizeText(request.getDescription()));
        department.setStatus(DepartmentStatus.ACTIVE);
        departmentMapper.insert(department);
        return DepartmentVO.from(departmentMapper.selectById(department.getId()));
    }

    @Override
    @Transactional
    public DepartmentVO updateDepartment(Long departmentId, UpdateDepartmentDTO request) {
        Department department = requireDepartment(departmentId);
        String name = requireDepartmentName(request == null ? null : request.getName());
        if (departmentMapper.countByName(name, departmentId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门名称已存在");
        }
        departmentMapper.updateDepartment(department.getId(), name, normalizeText(request.getDescription()));
        return DepartmentVO.from(departmentMapper.selectById(department.getId()));
    }

    @Override
    @Transactional
    public void disableDepartment(Long departmentId) {
        Department department = requireDepartment(departmentId);
        departmentMapper.updateStatus(department.getId(), DepartmentStatus.DISABLED);
    }

    @Override
    @Transactional
    public void enableDepartment(Long departmentId) {
        Department department = requireDepartment(departmentId);
        departmentMapper.updateStatus(department.getId(), DepartmentStatus.ACTIVE);
    }

    @Override
    public List<ClubMemberVO> listMembers(UserPrincipal principal, Long departmentId) {
        if (hasGlobalOrganizationScope(principal)) {
            return clubMemberMapper.selectMemberVOList(departmentId);
        }
        List<Long> managedDepartmentIds = managedDepartmentIds(principal);
        if (departmentId != null) {
            ensureDepartmentScope(principal, departmentId);
            return clubMemberMapper.selectMemberVOList(departmentId);
        }
        List<ClubMemberVO> members = new ArrayList<>();
        for (Long managedDepartmentId : managedDepartmentIds) {
            members.addAll(clubMemberMapper.selectMemberVOList(managedDepartmentId));
        }
        return members;
    }

    @Override
    public PageVO<AdminUserVO> listUsers(String keyword, Long departmentId, int page, int size) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        int offset = (normalizedPage - 1) * normalizedSize;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return new PageVO<>(
                userMapper.selectAdminUserPage(normalizedKeyword, departmentId, offset, normalizedSize),
                userMapper.countAdminUsers(normalizedKeyword, departmentId),
                normalizedPage,
                normalizedSize
        );
    }

    @Override
    @Transactional
    public ClubMemberVO assignMemberToDepartment(UserPrincipal principal, AssignMemberDepartmentDTO request) {
        if (request == null || request.getUserId() == null || request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 和部门 ID 不能为空");
        }
        requireUser(request.getUserId());
        requireActiveDepartment(request.getDepartmentId());
        ensureDepartmentScope(principal, request.getDepartmentId());
        clubMemberMapper.upsertMember(request.getUserId(), request.getDepartmentId(), MemberStatus.ACTIVE);
        roleMapper.deleteUserRoleByCode(request.getUserId(), REGISTERED_USER_ROLE);
        roleMapper.insertUserRoleByCode(request.getUserId(), CLUB_MEMBER_ROLE);
        return clubMemberMapper.selectMemberVOByUserId(request.getUserId());
    }

    @Override
    @Transactional
    public ClubMemberVO updateMemberStatus(UserPrincipal principal, UpdateMemberStatusDTO request) {
        if (request == null || request.getUserId() == null || request.getStatus() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 和成员状态不能为空");
        }
        ClubMemberVO member = clubMemberMapper.selectMemberVOByUserId(request.getUserId());
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        }
        ensureDepartmentScope(principal, member.getDepartmentId());
        clubMemberMapper.updateStatus(request.getUserId(), request.getStatus());
        if (request.getStatus() == MemberStatus.ACTIVE) {
            roleMapper.deleteUserRoleByCode(request.getUserId(), REGISTERED_USER_ROLE);
            roleMapper.insertUserRoleByCode(request.getUserId(), CLUB_MEMBER_ROLE);
        } else {
            departmentLeaderMapper.deleteByUserId(request.getUserId());
            roleMapper.deleteUserRoleByCode(request.getUserId(), DEPARTMENT_LEADER_ROLE);
            roleMapper.deleteUserRoleByCode(request.getUserId(), CLUB_MEMBER_ROLE);
            roleMapper.insertUserRoleByCode(request.getUserId(), REGISTERED_USER_ROLE);
        }
        return clubMemberMapper.selectMemberVOByUserId(request.getUserId());
    }

    @Override
    public List<DepartmentLeaderVO> listLeaders(UserPrincipal principal, Long departmentId) {
        if (hasGlobalOrganizationScope(principal)) {
            return departmentLeaderMapper.selectLeaderVOList(departmentId);
        }
        List<Long> managedDepartmentIds = managedDepartmentIds(principal);
        if (departmentId != null) {
            ensureDepartmentScope(principal, departmentId);
            return departmentLeaderMapper.selectLeaderVOList(departmentId);
        }
        List<DepartmentLeaderVO> leaders = new ArrayList<>();
        for (Long managedDepartmentId : managedDepartmentIds) {
            leaders.addAll(departmentLeaderMapper.selectLeaderVOList(managedDepartmentId));
        }
        return leaders;
    }

    @Override
    @Transactional
    public void appointDepartmentLeader(AssignDepartmentLeaderDTO request) {
        if (request == null || request.getUserId() == null || request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 和部门 ID 不能为空");
        }
        requireUser(request.getUserId());
        requireActiveDepartment(request.getDepartmentId());
        clubMemberMapper.upsertMember(request.getUserId(), request.getDepartmentId(), MemberStatus.ACTIVE);
        departmentLeaderMapper.insertLeader(request.getUserId(), request.getDepartmentId());
        roleMapper.deleteUserRoleByCode(request.getUserId(), REGISTERED_USER_ROLE);
        roleMapper.insertUserRoleByCode(request.getUserId(), CLUB_MEMBER_ROLE);
        roleMapper.insertUserRoleByCode(request.getUserId(), DEPARTMENT_LEADER_ROLE);
    }

    @Override
    @Transactional
    public void removeDepartmentLeader(AssignDepartmentLeaderDTO request) {
        if (request == null || request.getUserId() == null || request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 和部门 ID 不能为空");
        }
        departmentLeaderMapper.deleteLeader(request.getUserId(), request.getDepartmentId());
        if (departmentLeaderMapper.countByUserId(request.getUserId()) == 0) {
            roleMapper.deleteUserRoleByCode(request.getUserId(), DEPARTMENT_LEADER_ROLE);
        }
    }

    private String requireDepartmentName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部门名称不能为空");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 80) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部门名称长度不能超过 80 个字符");
        }
        return trimmedName;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private Department requireDepartment(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return department;
    }

    private Department requireActiveDepartment(Long departmentId) {
        Department department = requireDepartment(departmentId);
        if (department.getStatus() != DepartmentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门已停用");
        }
        return department;
    }

    private boolean hasGlobalOrganizationScope(UserPrincipal principal) {
        return principal.permissions().contains("system:maintain")
                || principal.permissions().contains("department:manage");
    }

    private List<Long> managedDepartmentIds(UserPrincipal principal) {
        return departmentLeaderMapper.selectDepartmentIdsByUserId(principal.userId());
    }

    private void ensureDepartmentScope(UserPrincipal principal, Long departmentId) {
        if (hasGlobalOrganizationScope(principal)) {
            return;
        }
        if (!managedDepartmentIds(principal).contains(departmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理本部门成员");
        }
    }
}
