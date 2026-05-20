import { FormEvent, useEffect, useMemo, useState } from 'react'
import { changePassword, getProfile, updateProfile, type UserProfile } from '@/api/modules/auth'
import { saveUser } from '@/auth'
import PageLoading from '@/components/PageLoading'
import { getErrorMessage, showToast } from '@/toast'

const roleNames: Record<string, string> = {
  REGISTERED_USER: '注册用户',
  CLUB_MEMBER: '普通成员',
  DEPARTMENT_LEADER: '部门负责人',
  PRESIDENT: '社长',
  SYSTEM_MAINTAINER: '系统维护者',
}

const statusNames: Record<UserProfile['status'], string> = {
  NORMAL: '正常',
  DISABLED: '禁用',
  DELETED: '删除',
}

export default function ProfileView() {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [nickname, setNickname] = useState('')
  const [email, setEmail] = useState('')
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)
  const [profileModalOpen, setProfileModalOpen] = useState(false)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)

  const primaryRole = useMemo(() => {
    if (!user?.roles.length) {
      return '暂无角色'
    }
    return user.roles.map((role) => roleNames[role] || role).join(' / ')
  }, [user])

  useEffect(() => {
    let active = true
    getProfile()
      .then((profile) => {
        if (!active) {
          return
        }
        setUser(profile)
        setNickname(profile.nickname || '')
        setEmail(profile.email || '')
        saveUser(profile)
      })
      .catch((err) => {
        if (active) {
          setError(getErrorMessage(err, '获取个人资料失败'))
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

  function openProfileModal() {
    if (!user) {
      return
    }
    setNickname(user.nickname || '')
    setEmail(user.email || '')
    setError('')
    setProfileModalOpen(true)
  }

  function closeProfileModal() {
    setProfileModalOpen(false)
    setSavingProfile(false)
  }

  function openPasswordModal() {
    setOldPassword('')
    setNewPassword('')
    setConfirmPassword('')
    setError('')
    setPasswordModalOpen(true)
  }

  function closePasswordModal() {
    setPasswordModalOpen(false)
    setSavingPassword(false)
  }

  async function handleProfileSubmit(event: FormEvent) {
    event.preventDefault()
    setSavingProfile(true)
    setError('')
    try {
      const profile = await updateProfile({ nickname, email: email || null })
      setUser(profile)
      setNickname(profile.nickname || '')
      setEmail(profile.email || '')
      saveUser(profile)
      closeProfileModal()
      showToast('个人资料已更新', 'success')
    } catch (err) {
      setError('')
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(event: FormEvent) {
    event.preventDefault()
    setSavingPassword(true)
    setError('')
    if (newPassword !== confirmPassword) {
      setError('两次输入的新密码不一致')
      setSavingPassword(false)
      return
    }
    try {
      await changePassword(oldPassword, newPassword)
      closePasswordModal()
      showToast('密码已更新', 'success')
    } catch (err) {
      setError(getErrorMessage(err, '密码更新失败'))
    } finally {
      setSavingPassword(false)
    }
  }

  if (loading) {
    return <PageLoading />
  }

  if (!user) {
    return <section className="content">{error || '无法获取个人资料'}</section>
  }

  return (
    <section className="content profile-page">
      <div className="profile-header">
        <div>
          <p className="profile-eyebrow">Account</p>
          <h1>个人资料</h1>
        </div>
        <div className="profile-actions">
          <button className="secondary-button" onClick={openPasswordModal} type="button">
            修改密码
          </button>
          <button onClick={openProfileModal} type="button">
            修改
          </button>
        </div>
      </div>

      {error && !profileModalOpen && !passwordModalOpen && <p className="error">{error}</p>}

      <section className="profile-summary">
        <div className="profile-avatar">{(user.nickname || user.username).slice(0, 1).toUpperCase()}</div>
        <div>
          <h2>{user.nickname || user.username}</h2>
          <p>{primaryRole}</p>
        </div>
        <span className="status-pill">{statusNames[user.status]}</span>
      </section>

      <div className="profile-sections">
        <section className="profile-panel profile-panel-wide">
          <div className="section-heading">
            <h2>基础信息</h2>
          </div>
          <dl className="profile-list">
            <div>
              <dt>用户名</dt>
              <dd>{user.username}</dd>
            </div>
            <div>
              <dt>昵称</dt>
              <dd>{user.nickname}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{user.email || '未填写'}</dd>
            </div>
            <div>
              <dt>账号状态</dt>
              <dd>{statusNames[user.status]}</dd>
            </div>
          </dl>
        </section>

        <section className="profile-panel">
          <div className="section-heading">
            <h2>权限信息</h2>
          </div>
          <div className="pill-row">
            {user.roles.length ? (
              user.roles.map((role) => <span key={role}>{roleNames[role] || role}</span>)
            ) : (
              <span>暂无角色</span>
            )}
          </div>
        </section>

        {user.membership && (
          <section className="profile-panel">
            <div className="section-heading">
              <h2>社团信息</h2>
            </div>
            <dl className="profile-list">
              <div>
                <dt>部门</dt>
                <dd>{user.membership.departmentName}</dd>
              </div>
              <div>
                <dt>加入时间</dt>
                <dd>{new Date(user.membership.joinedAt).toLocaleString()}</dd>
              </div>
            </dl>
          </section>
        )}
      </div>

      {profileModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel" onSubmit={handleProfileSubmit}>
            <div className="modal-header">
              <h2>修改个人资料</h2>
              <button className="icon-button" onClick={closeProfileModal} type="button">
                x
              </button>
            </div>
            <div className="profile-form">
              <label>
                用户名
                <input disabled value={user.username} />
              </label>
              <label>
                账号状态
                <input disabled value={statusNames[user.status]} />
              </label>
              <label>
                昵称
                <input
                  maxLength={50}
                  onChange={(event) => setNickname(event.target.value)}
                  required
                  value={nickname}
                />
              </label>
              <label>
                邮箱
                <input
                  autoComplete="email"
                  maxLength={120}
                  onChange={(event) => setEmail(event.target.value)}
                  type="email"
                  value={email}
                />
              </label>
            </div>
            <div className="modal-actions">
              <button className="secondary-button" onClick={closeProfileModal} type="button">
                取消
              </button>
              <button disabled={savingProfile} type="submit">
                {savingProfile ? '保存中...' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}

      {passwordModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel modal-panel-narrow" onSubmit={handlePasswordSubmit}>
            <div className="modal-header">
              <h2>修改密码</h2>
              <button className="icon-button" onClick={closePasswordModal} type="button">
                x
              </button>
            </div>
            <div className="profile-form">
              <label>
                旧密码
                <input
                  autoComplete="current-password"
                  onChange={(event) => setOldPassword(event.target.value)}
                  required
                  type="password"
                  value={oldPassword}
                />
              </label>
              <label>
                新密码
                <input
                  autoComplete="new-password"
                  minLength={8}
                  onChange={(event) => setNewPassword(event.target.value)}
                  required
                  type="password"
                  value={newPassword}
                />
              </label>
              <label>
                确认新密码
                <input
                  autoComplete="new-password"
                  minLength={8}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  required
                  type="password"
                  value={confirmPassword}
                />
              </label>
            </div>
            {error && <p className="form-error">{error}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={closePasswordModal} type="button">
                取消
              </button>
              <button disabled={savingPassword} type="submit">
                {savingPassword ? '验证中...' : '确认修改'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
