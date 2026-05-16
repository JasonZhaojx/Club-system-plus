import { FormEvent, useEffect, useMemo, useState } from 'react'
import { getCurrentUser, type UserProfile } from '@/api/modules/auth'
import {
  appointDepartmentLeader,
  assignMemberToDepartment,
  createDepartment,
  disableDepartment,
  enableDepartment,
  listDepartmentLeaders,
  listDepartments,
  listMembers,
  listUsers,
  removeDepartmentLeader,
  updateDepartment,
  updateMemberStatus,
  type ClubMember,
  type AdminUser,
  type Department,
  type MemberStatus,
} from '@/api/modules/organization'
import { assignUserRoles, listRoles, type Role } from '@/api/modules/rbac'
import { hasPermission } from '@/auth'
import { getErrorMessage, showToast } from '@/toast'

type AdminTab = 'members' | 'departments'

const memberStatusNames: Record<MemberStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '禁用',
  REMOVED: '移除',
}

const roleNames: Record<string, string> = {
  REGISTERED_USER: '注册用户',
  CLUB_MEMBER: '普通成员',
  DEPARTMENT_LEADER: '部门负责人',
  PRESIDENT: '社长',
  SYSTEM_MAINTAINER: '系统维护者',
}

export default function AdminView() {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [departments, setDepartments] = useState<Department[]>([])
  const [members, setMembers] = useState<ClubMember[]>([])
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([])
  const [userPage, setUserPage] = useState(1)
  const [userTotal, setUserTotal] = useState(0)
  const [userKeyword, setUserKeyword] = useState('')
  const [roles, setRoles] = useState<Role[]>([])
  const [activeTab, setActiveTab] = useState<AdminTab>('members')
  const [selectedDepartmentId, setSelectedDepartmentId] = useState('')
  const [memberModalOpen, setMemberModalOpen] = useState(false)
  const [departmentModalOpen, setDepartmentModalOpen] = useState(false)
  const [editingMember, setEditingMember] = useState<ClubMember | null>(null)
  const [editingDepartment, setEditingDepartment] = useState<Department | null>(null)
  const [memberDepartmentId, setMemberDepartmentId] = useState('')
  const [memberStatus, setMemberStatus] = useState<MemberStatus>('ACTIVE')
  const [memberRoleCode, setMemberRoleCode] = useState('CLUB_MEMBER')
  const [memberLeader, setMemberLeader] = useState(false)
  const [newMemberUserId, setNewMemberUserId] = useState('')
  const [departmentName, setDepartmentName] = useState('')
  const [departmentDescription, setDepartmentDescription] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const canManageDepartments = hasPermission(user, 'department:manage')
  const canManageRoles = hasPermission(user, 'department:manage')
  const canManageMembers = hasPermission(user, 'member:manage')

  const visibleDepartments = useMemo(() => {
    if (canManageDepartments) {
      return departments
    }
    const visibleDepartmentIds = new Set(members.map((member) => member.departmentId))
    return departments.filter((department) => visibleDepartmentIds.has(department.id))
  }, [canManageDepartments, departments, members])

  useEffect(() => {
    let active = true
    Promise.all([getCurrentUser(), listDepartments(), listMembers()])
      .then(async ([currentUser, departmentList, memberList]) => {
        if (!active) {
          return
        }
        setUser(currentUser)
        setDepartments(departmentList)
        setMembers(memberList)
        if (currentUser.permissions.includes('department:manage') || currentUser.permissions.includes('system:maintain')) {
          setRoles(await listRoles())
          const users = await listUsers(1, 10)
          setAdminUsers(users.records)
          setUserPage(users.page)
          setUserTotal(users.total)
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [])

  async function refreshMembers(departmentId = selectedDepartmentId) {
    const nextMembers = await listMembers(departmentId ? Number(departmentId) : undefined)
    setMembers(nextMembers)
  }

  async function refreshUsers(page = userPage) {
    if (!canManageDepartments) {
      return
    }
    const users = await listUsers(
      page,
      10,
      userKeyword,
      selectedDepartmentId ? Number(selectedDepartmentId) : undefined,
    )
    setAdminUsers(users.records)
    setUserPage(users.page)
    setUserTotal(users.total)
  }

  async function refreshDepartments() {
    setDepartments(await listDepartments())
  }

  async function handleDepartmentFilterChange(value: string) {
    setSelectedDepartmentId(value)
    if (canManageDepartments) {
      const users = await listUsers(1, 10, userKeyword, value ? Number(value) : undefined)
      setAdminUsers(users.records)
      setUserPage(users.page)
      setUserTotal(users.total)
      return
    }
    await refreshMembers(value)
  }

  function openAddMemberModal() {
    setEditingMember(null)
    setNewMemberUserId('')
    setMemberDepartmentId(selectedDepartmentId || '')
    setMemberStatus('ACTIVE')
    setMemberRoleCode('CLUB_MEMBER')
    setMemberLeader(false)
    setFormError('')
    setMemberModalOpen(true)
  }

  function openEditUserModal(adminUser: AdminUser) {
    setEditingMember(
      adminUser.departmentId
        ? {
            userId: adminUser.id,
            username: adminUser.username,
            nickname: adminUser.nickname,
            departmentId: adminUser.departmentId,
            departmentName: adminUser.departmentName || '',
            joinedAt: adminUser.joinedAt || new Date().toISOString(),
            status: adminUser.memberStatus || 'ACTIVE',
            departmentLeader: adminUser.departmentLeader,
          }
        : null,
    )
    setNewMemberUserId(String(adminUser.id))
    setMemberDepartmentId(adminUser.departmentId ? String(adminUser.departmentId) : '')
    setMemberStatus(adminUser.memberStatus || 'ACTIVE')
    setMemberRoleCode(resolvePrimaryRole(adminUser.roles))
    setMemberLeader(Boolean(adminUser.departmentLeader))
    setFormError('')
    setMemberModalOpen(true)
  }

  function openEditMemberModal(member: ClubMember) {
    setEditingMember(member)
    setNewMemberUserId(String(member.userId))
    setMemberDepartmentId(String(member.departmentId))
    setMemberStatus(member.status)
    setMemberRoleCode('CLUB_MEMBER')
    setMemberLeader(member.departmentLeader)
    setFormError('')
    setMemberModalOpen(true)
  }

  function openDepartmentModal(department?: Department) {
    setEditingDepartment(department || null)
    setDepartmentName(department?.name || '')
    setDepartmentDescription(department?.description || '')
    setFormError('')
    setDepartmentModalOpen(true)
  }

  function resolvePrimaryRole(roles?: string | null) {
    const roleList = roles ? roles.split(',').filter(Boolean) : []
    if (roleList.includes('SYSTEM_MAINTAINER')) return 'SYSTEM_MAINTAINER'
    if (roleList.includes('PRESIDENT')) return 'PRESIDENT'
    if (roleList.includes('DEPARTMENT_LEADER')) return 'DEPARTMENT_LEADER'
    if (roleList.includes('CLUB_MEMBER')) return 'CLUB_MEMBER'
    return roleList[0] || 'CLUB_MEMBER'
  }

  async function handleMemberSubmit(event: FormEvent) {
    event.preventDefault()
    if (!memberDepartmentId || !newMemberUserId) {
      setFormError('用户 ID 和部门不能为空')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      const userId = Number(newMemberUserId)
      const departmentId = Number(memberDepartmentId)
      await assignMemberToDepartment({ userId, departmentId })
      if (editingMember) {
        await updateMemberStatus({ userId, status: memberStatus })
      }
      if (canManageRoles && memberRoleCode) {
        await assignUserRoles({ userId, roleCodes: [memberRoleCode] })
      }
      if (canManageDepartments) {
        if (memberLeader) {
          await appointDepartmentLeader({ userId, departmentId })
        } else if (editingMember?.departmentLeader) {
          await removeDepartmentLeader({ userId, departmentId })
        }
      }
      setMemberModalOpen(false)
      await refreshMembers()
      await refreshUsers()
      showToast(editingMember ? '成员信息已更新' : '成员已加入部门', 'success')
    } catch (err) {
      setFormError(getErrorMessage(err, '成员保存失败'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDepartmentSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editingDepartment) {
        await updateDepartment(editingDepartment.id, {
          name: departmentName,
          description: departmentDescription || null,
        })
      } else {
        await createDepartment({
          name: departmentName,
          description: departmentDescription || null,
        })
      }
      setDepartmentModalOpen(false)
      await refreshDepartments()
      showToast(editingDepartment ? '部门已更新' : '部门已创建', 'success')
    } catch (err) {
      setFormError(getErrorMessage(err, '部门保存失败'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDisableDepartment(department: Department) {
    await disableDepartment(department.id)
    await refreshDepartments()
    showToast('部门已停用', 'success')
  }

  async function handleEnableDepartment(department: Department) {
    await enableDepartment(department.id)
    await refreshDepartments()
    showToast('部门已启用', 'success')
  }

  async function handleShowLeaders(department: Department) {
    const leaders = await listDepartmentLeaders(department.id)
    const names = leaders.map((leader) => leader.nickname || leader.username).join('、')
    showToast(names ? `${department.name} 负责人：${names}` : `${department.name} 暂无负责人`, 'success')
  }

  async function handleUserPageChange(nextPage: number) {
    if (nextPage < 1 || nextPage > Math.ceil(userTotal / 10)) {
      return
    }
    await refreshUsers(nextPage)
  }

  async function handleUserSearch(event: FormEvent) {
    event.preventDefault()
    await refreshUsers(1)
  }

  if (loading) {
    return <section className="content">正在加载后台数据...</section>
  }

  return (
    <section className="content admin-page">
      <div className="admin-header">
        <div>
          <p className="profile-eyebrow">Admin</p>
          <h1>管理后台</h1>
          {user && <p>当前用户：{user.nickname || user.username}</p>}
        </div>
      </div>

      <div className="admin-tabs">
        {canManageMembers && (
          <button
            className={activeTab === 'members' ? 'active' : ''}
            onClick={() => setActiveTab('members')}
            type="button"
          >
            成员与权限
          </button>
        )}
        {canManageDepartments && (
          <button
            className={activeTab === 'departments' ? 'active' : ''}
            onClick={() => setActiveTab('departments')}
            type="button"
          >
            部门管理
          </button>
        )}
      </div>

      {activeTab === 'members' && canManageMembers && (
        <section className="admin-stack">
          {canManageDepartments && (
            <div className="admin-panel">
              <form className="admin-toolbar" onSubmit={handleUserSearch}>
                <select
                  onChange={(event) => void handleDepartmentFilterChange(event.target.value)}
                  value={selectedDepartmentId}
                >
                  <option value="">全部用户</option>
                  {visibleDepartments.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name}
                    </option>
                  ))}
                </select>
                <div className="admin-search">
                  <input
                    onChange={(event) => setUserKeyword(event.target.value)}
                    placeholder="按用户名查询"
                    value={userKeyword}
                  />
                  <button type="submit">查询</button>
                </div>
              </form>
              <div className="admin-subtoolbar">每页 10 个，共 {userTotal} 个</div>
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>用户</th>
                      <th>邮箱</th>
                      <th>部门</th>
                      <th>角色</th>
                      <th>状态</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {adminUsers.map((adminUser) => (
                      <tr key={adminUser.id}>
                        <td>#{adminUser.id}</td>
                        <td>
                          <strong>{adminUser.nickname || adminUser.username}</strong>
                          <span>{adminUser.username}</span>
                        </td>
                        <td>{adminUser.email || '未填写'}</td>
                        <td>{adminUser.departmentName || '未加入部门'}</td>
                        <td>
                          {adminUser.roles
                            ? adminUser.roles
                                .split(',')
                                .map((role) => roleNames[role] || role)
                                .join('、')
                            : '暂无角色'}
                        </td>
                        <td>{adminUser.status}</td>
                        <td>
                          <button className="secondary-button" onClick={() => openEditUserModal(adminUser)} type="button">
                            调整
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="pagination">
                <button
                  className="secondary-button"
                  disabled={userPage <= 1}
                  onClick={() => void handleUserPageChange(userPage - 1)}
                  type="button"
                >
                  上一页
                </button>
                <span>
                  第 {userPage} / {Math.max(Math.ceil(userTotal / 10), 1)} 页
                </span>
                <button
                  className="secondary-button"
                  disabled={userPage >= Math.ceil(userTotal / 10)}
                  onClick={() => void handleUserPageChange(userPage + 1)}
                  type="button"
                >
                  下一页
                </button>
              </div>
            </div>
          )}

          {!canManageDepartments && (
            <div className="admin-panel">
          <div className="admin-toolbar">
            <select
              onChange={(event) => void handleDepartmentFilterChange(event.target.value)}
              value={selectedDepartmentId}
            >
              <option value="">全部可管理部门</option>
              {visibleDepartments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
            <button onClick={openAddMemberModal} type="button">
              添加成员
            </button>
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>部门</th>
                  <th>加入时间</th>
                  <th>状态</th>
                  <th>负责人</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr key={member.userId}>
                    <td>
                      <strong>{member.nickname || member.username}</strong>
                      <span>#{member.userId}</span>
                    </td>
                    <td>{member.departmentName}</td>
                    <td>{new Date(member.joinedAt).toLocaleDateString()}</td>
                    <td>{memberStatusNames[member.status]}</td>
                    <td>{member.departmentLeader ? '是' : '否'}</td>
                    <td>
                      <button className="secondary-button" onClick={() => openEditMemberModal(member)} type="button">
                        调整
                      </button>
                    </td>
                  </tr>
                ))}
                {!members.length && (
                  <tr>
                    <td colSpan={6}>暂无成员数据</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          </div>
          )}
        </section>
      )}

      {activeTab === 'departments' && canManageDepartments && (
        <section className="admin-panel">
          <div className="admin-toolbar">
            <span>部门用于限制负责人管理范围和后续活动审核范围。</span>
            <button onClick={() => openDepartmentModal()} type="button">
              新建部门
            </button>
          </div>
          <div className="data-table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>部门</th>
                  <th>说明</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {departments.map((department) => (
                  <tr key={department.id}>
                    <td>{department.name}</td>
                    <td>{department.description || '未填写'}</td>
                    <td>{department.status === 'ACTIVE' ? '启用' : '停用'}</td>
                    <td>
                      <div className="table-actions">
                        <button className="secondary-button" onClick={() => openDepartmentModal(department)} type="button">
                          编辑
                        </button>
                        <button className="secondary-button" onClick={() => void handleShowLeaders(department)} type="button">
                          负责人
                        </button>
                        {department.status === 'ACTIVE' && (
                          <button className="danger-button" onClick={() => void handleDisableDepartment(department)} type="button">
                            停用
                          </button>
                        )}
                        {department.status === 'DISABLED' && (
                          <button className="secondary-button" onClick={() => void handleEnableDepartment(department)} type="button">
                            启用
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {memberModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel" onSubmit={handleMemberSubmit}>
            <div className="modal-header">
              <h2>{editingMember ? '调整成员' : '添加成员'}</h2>
              <button className="icon-button" onClick={() => setMemberModalOpen(false)} type="button">
                x
              </button>
            </div>
            <div className="profile-form">
              <label>
                用户 ID
                <input
                  disabled={Boolean(editingMember)}
                  onChange={(event) => setNewMemberUserId(event.target.value)}
                  required
                  type="number"
                  value={newMemberUserId}
                />
              </label>
              <label>
                部门
                <select
                  onChange={(event) => setMemberDepartmentId(event.target.value)}
                  required
                  value={memberDepartmentId}
                >
                  <option value="">请选择部门</option>
                  {visibleDepartments
                    .filter((department) => department.status === 'ACTIVE')
                    .map((department) => (
                      <option key={department.id} value={department.id}>
                        {department.name}
                      </option>
                    ))}
                </select>
              </label>
              {editingMember && (
                <label>
                  成员状态
                  <select
                    onChange={(event) => setMemberStatus(event.target.value as MemberStatus)}
                    value={memberStatus}
                  >
                    <option value="ACTIVE">正常</option>
                    <option value="DISABLED">禁用</option>
                    <option value="REMOVED">移除</option>
                  </select>
                </label>
              )}
              {canManageDepartments && (
                <label className="checkbox-row">
                  <input
                    checked={memberLeader}
                    onChange={(event) => setMemberLeader(event.target.checked)}
                    type="checkbox"
                  />
                  设为该部门负责人
                </label>
              )}
              {canManageRoles && (
                <div className="role-checks">
                  <span>角色身份</span>
                  {roles.map((role) => (
                    <label key={role.code} className="checkbox-row">
                      <input
                        checked={memberRoleCode === role.code}
                        onChange={() => setMemberRoleCode(role.code)}
                        type="radio"
                      />
                      {roleNames[role.code] || role.name || role.code}
                    </label>
                  ))}
                </div>
              )}
            </div>
            {formError && <p className="form-error">{formError}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setMemberModalOpen(false)} type="button">
                取消
              </button>
              <button disabled={saving} type="submit">
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}

      {departmentModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel" onSubmit={handleDepartmentSubmit}>
            <div className="modal-header">
              <h2>{editingDepartment ? '编辑部门' : '新建部门'}</h2>
              <button className="icon-button" onClick={() => setDepartmentModalOpen(false)} type="button">
                x
              </button>
            </div>
            <div className="profile-form">
              <label>
                部门名称
                <input
                  maxLength={80}
                  onChange={(event) => setDepartmentName(event.target.value)}
                  required
                  value={departmentName}
                />
              </label>
              <label>
                部门说明
                <textarea
                  onChange={(event) => setDepartmentDescription(event.target.value)}
                  rows={4}
                  value={departmentDescription}
                />
              </label>
            </div>
            {formError && <p className="form-error">{formError}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setDepartmentModalOpen(false)} type="button">
                取消
              </button>
              <button disabled={saving} type="submit">
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
