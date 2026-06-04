import { FormEvent, useEffect, useState } from 'react'
import {
  cancelActivity,
  createActivity,
  finishActivity,
  listManageActivities,
  publishActivity,
  submitActivity,
  updateActivity,
  type Activity,
  type ActivityPayload,
  type ActivityStatus,
} from '@/api/modules/activity'
import { hasPermission } from '@/auth'
import type { UserProfile } from '@/api/modules/auth'
import { uploadImage } from '@/api/modules/file'
import { getErrorMessage, showToast } from '@/toast'

const statusNames: Record<ActivityStatus, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  PUBLISHED: '已发布',
  CANCELLED: '已取消',
  ENDED: '已结束',
}

const emptyForm = {
  title: '',
  summary: '',
  detail: '',
  category: 'social',
  categoryName: '娱乐·交友',
  imageUrl: '',
  location: '',
  startTime: '',
  endTime: '',
  capacity: 50,
  requiredRoleCode: '',
}

interface ActivityFormState {
  title: string
  summary: string
  detail: string
  category: string
  categoryName: string
  imageUrl: string
  location: string
  startTime: string
  endTime: string
  capacity: number
  requiredRoleCode: string
}

function toDatetimeLocal(value: string) {
  return value ? value.slice(0, 16) : ''
}

function toPayload(form: ActivityFormState): ActivityPayload {
  return {
    title: form.title,
    summary: form.summary,
    detail: form.detail,
    category: form.category,
    categoryName: form.categoryName,
    imageUrl: form.imageUrl || null,
    location: form.location,
    startTime: form.startTime,
    endTime: form.endTime,
    capacity: Number(form.capacity),
    requiredRoleCode: form.requiredRoleCode || null,
  }
}

export default function ActivityAdminPanel({ user }: { user: UserProfile | null }) {
  const [activities, setActivities] = useState<Activity[]>([])
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<ActivityStatus | ''>('')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingActivity, setEditingActivity] = useState<Activity | null>(null)
  const [form, setForm] = useState<ActivityFormState>(emptyForm)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [uploadingImage, setUploadingImage] = useState(false)

  const canCreate = hasPermission(user, 'activity:create')
  const canUpdate = hasPermission(user, 'activity:update')
  const canReview = hasPermission(user, 'activity:review')
  const canCancel = hasPermission(user, 'activity:cancel') || canReview

  useEffect(() => {
    void refreshActivities(1)
  }, [status])

  async function refreshActivities(nextPage = page) {
    const result = await listManageActivities({
      page: nextPage,
      size: 10,
      keyword: keyword.trim() || undefined,
      status: status || undefined,
      sort: 'latest',
    })
    setActivities(result.records)
    setPage(result.page)
    setTotal(result.total)
  }

  function openCreateModal() {
    setEditingActivity(null)
    setForm(emptyForm)
    setFormError('')
    setModalOpen(true)
  }

  function openEditModal(activity: Activity) {
    setEditingActivity(activity)
    setForm({
      title: activity.title,
      summary: activity.summary,
      detail: activity.detail,
      category: activity.category,
      categoryName: activity.categoryName,
      imageUrl: activity.imageUrl || '',
      location: activity.location,
      startTime: toDatetimeLocal(activity.startTime),
      endTime: toDatetimeLocal(activity.endTime),
      capacity: activity.capacity,
      requiredRoleCode: activity.requiredRoleCode || '',
    })
    setFormError('')
    setModalOpen(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editingActivity) {
        await updateActivity(editingActivity.id, toPayload(form))
      } else {
        await createActivity(toPayload(form))
      }
      setModalOpen(false)
      await refreshActivities(1)
      showToast(editingActivity ? '活动已更新' : '活动草稿已创建', 'success')
    } catch (err) {
      setFormError(getErrorMessage(err, '活动保存失败'))
    } finally {
      setSaving(false)
    }
  }

  async function handleImageUpload(file: File | undefined) {
    if (!file) {
      return
    }
    setUploadingImage(true)
    setFormError('')
    try {
      const result = await uploadImage(file, 'activity')
      setForm((current) => ({ ...current, imageUrl: result.url }))
      showToast('图片已上传', 'success')
    } catch (err) {
      setFormError(getErrorMessage(err, '图片上传失败'))
    } finally {
      setUploadingImage(false)
    }
  }

  async function runAction(action: () => Promise<Activity>, message: string) {
    await action()
    await refreshActivities()
    showToast(message, 'success')
  }

  return (
    <section className="admin-panel">
      <form className="admin-toolbar" onSubmit={(event) => {
        event.preventDefault()
        void refreshActivities(1)
      }}>
        <select onChange={(event) => setStatus(event.target.value as ActivityStatus | '')} value={status}>
          <option value="">全部状态</option>
          {Object.entries(statusNames).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <div className="admin-search">
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="按活动标题搜索"
            value={keyword}
          />
          <button type="submit">查询</button>
        </div>
        {canCreate && <button onClick={openCreateModal} type="button">新建活动</button>}
      </form>

      <div className="data-table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>活动</th>
              <th>状态</th>
              <th>报名</th>
              <th>开始时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {activities.map((activity) => (
              <tr key={activity.id}>
                <td>
                  <strong>{activity.title}</strong>
                  <span>{activity.location}</span>
                </td>
                <td>{statusNames[activity.status]}</td>
                <td>{activity.registeredCount}/{activity.capacity}</td>
                <td>{new Date(activity.startTime).toLocaleString()}</td>
                <td>
                  <div className="table-actions">
                    {canUpdate && (
                      <button className="secondary-button" onClick={() => openEditModal(activity)} type="button">
                        编辑
                      </button>
                    )}
                    {canUpdate && activity.status === 'DRAFT' && (
                      <button className="secondary-button" onClick={() => void runAction(() => submitActivity(activity.id), '已提交审核')} type="button">
                        提审
                      </button>
                    )}
                    {canReview && (activity.status === 'DRAFT' || activity.status === 'PENDING_REVIEW') && (
                      <button className="secondary-button" onClick={() => void runAction(() => publishActivity(activity.id), '活动已发布')} type="button">
                        发布
                      </button>
                    )}
                    {canCancel && activity.status !== 'CANCELLED' && activity.status !== 'ENDED' && (
                      <button className="secondary-button" onClick={() => void runAction(() => cancelActivity(activity.id), '活动已取消')} type="button">
                        取消
                      </button>
                    )}
                    {canReview && activity.status === 'PUBLISHED' && (
                      <button className="secondary-button" onClick={() => void runAction(() => finishActivity(activity.id), '活动已结束')} type="button">
                        结束
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {!activities.length && (
              <tr>
                <td colSpan={5}>暂无活动数据</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination">
        <button disabled={page <= 1} onClick={() => void refreshActivities(page - 1)} type="button">
          上一页
        </button>
        <span>第 {page} 页 / 共 {Math.max(Math.ceil(total / 10), 1)} 页</span>
        <button disabled={page >= Math.ceil(total / 10)} onClick={() => void refreshActivities(page + 1)} type="button">
          下一页
        </button>
      </div>

      {modalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel activity-modal" onSubmit={handleSubmit}>
            <div className="modal-header">
              <h2>{editingActivity ? '编辑活动' : '新建活动'}</h2>
              <button className="icon-button" onClick={() => setModalOpen(false)} type="button">x</button>
            </div>
            <div className="form-grid">
              <label>
                标题
                <input onChange={(event) => setForm({ ...form, title: event.target.value })} value={form.title} />
              </label>
              <label>
                分类编码
                <input onChange={(event) => setForm({ ...form, category: event.target.value })} value={form.category} />
              </label>
              <label>
                分类名称
                <input onChange={(event) => setForm({ ...form, categoryName: event.target.value })} value={form.categoryName} />
              </label>
              <label>
                地点
                <input onChange={(event) => setForm({ ...form, location: event.target.value })} value={form.location} />
              </label>
              <label>
                开始时间
                <input onChange={(event) => setForm({ ...form, startTime: event.target.value })} type="datetime-local" value={form.startTime} />
              </label>
              <label>
                结束时间
                <input onChange={(event) => setForm({ ...form, endTime: event.target.value })} type="datetime-local" value={form.endTime} />
              </label>
              <label>
                名额
                <input min={1} onChange={(event) => setForm({ ...form, capacity: Number(event.target.value) })} type="number" value={form.capacity} />
              </label>
              <label>
                报名角色限制
                <select onChange={(event) => setForm({ ...form, requiredRoleCode: event.target.value })} value={form.requiredRoleCode}>
                  <option value="">无限制</option>
                  <option value="REGISTERED_USER">注册用户</option>
                  <option value="CLUB_MEMBER">普通成员</option>
                  <option value="DEPARTMENT_LEADER">部门负责人</option>
                  <option value="PRESIDENT">社长</option>
                </select>
              </label>
              <label className="form-grid-wide">
                图片 URL
                <input onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} value={form.imageUrl} />
              </label>
              <label className="form-grid-wide">
                上传图片
                <input
                  accept="image/jpeg,image/png,image/webp"
                  disabled={uploadingImage}
                  onChange={(event) => void handleImageUpload(event.target.files?.[0])}
                  type="file"
                />
              </label>
              {form.imageUrl && (
                <div className="form-grid-wide image-upload-preview">
                  <img alt="活动图片预览" src={form.imageUrl} />
                  <span>{uploadingImage ? '上传中...' : '当前活动图片'}</span>
                </div>
              )}
              <label className="form-grid-wide">
                摘要
                <textarea onChange={(event) => setForm({ ...form, summary: event.target.value })} rows={3} value={form.summary} />
              </label>
              <label className="form-grid-wide">
                详情
                <textarea onChange={(event) => setForm({ ...form, detail: event.target.value })} rows={6} value={form.detail} />
              </label>
            </div>
            {formError && <p className="form-error">{formError}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setModalOpen(false)} type="button">取消</button>
              <button disabled={saving} type="submit">保存</button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
