import { FormEvent, useEffect, useState } from 'react'
import {
  createCouponBatch,
  listManageCouponBatches,
  updateCouponBatch,
  type CouponBatch,
  type CouponBatchPayload,
  type CouponBatchStatus,
} from '@/api/modules/coupon'
import { getErrorMessage, showToast } from '@/toast'

const statusNames: Record<CouponBatchStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '可领取',
  DISABLED: '已停用',
}

const emptyForm = {
  name: '',
  description: '',
  couponType: 'BENEFIT',
  benefitText: '',
  stock: 100,
  claimStartTime: '',
  claimEndTime: '',
  expireTime: '',
  allowedRoleCodes: ['REGISTERED_USER'],
}

function toPayload(form: typeof emptyForm): CouponBatchPayload {
  return {
    name: form.name,
    description: form.description || null,
    couponType: form.couponType,
    benefitText: form.benefitText,
    stock: Number(form.stock),
    claimStartTime: form.claimStartTime,
    claimEndTime: form.claimEndTime,
    expireTime: form.expireTime,
    allowedRoleCodes: form.allowedRoleCodes,
  }
}

export default function CouponAdminPanel() {
  const [batches, setBatches] = useState<CouponBatch[]>([])
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingBatch, setEditingBatch] = useState<CouponBatch | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    void refresh(1)
  }, [])

  async function refresh(nextPage = page) {
    const result = await listManageCouponBatches({
      page: nextPage,
      size: 10,
      keyword: keyword.trim() || undefined,
    })
    setBatches(result.records)
    setPage(result.page)
    setTotal(result.total)
  }

  function toggleRole(role: string) {
    setForm((current) => {
      const roles = current.allowedRoleCodes.includes(role)
        ? current.allowedRoleCodes.filter((item) => item !== role)
        : [...current.allowedRoleCodes, role]
      return { ...current, allowedRoleCodes: roles }
    })
  }

  function openCreateModal() {
    setEditingBatch(null)
    setForm(emptyForm)
    setFormError('')
    setModalOpen(true)
  }

  function openEditModal(batch: CouponBatch) {
    setEditingBatch(batch)
    setForm({
      name: batch.name,
      description: batch.description || '',
      couponType: batch.couponType,
      benefitText: batch.benefitText,
      stock: batch.stock,
      claimStartTime: batch.claimStartTime.slice(0, 16),
      claimEndTime: batch.claimEndTime.slice(0, 16),
      expireTime: batch.expireTime.slice(0, 16),
      allowedRoleCodes: batch.allowedRoleCodes,
    })
    setFormError('')
    setModalOpen(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editingBatch) {
        await updateCouponBatch(editingBatch.id, toPayload(form))
      } else {
        await createCouponBatch(toPayload(form))
      }
      setModalOpen(false)
      setEditingBatch(null)
      setForm(emptyForm)
      await refresh(1)
      showToast(editingBatch ? '优惠券批次已更新' : '优惠券批次已创建', 'success')
    } catch (err) {
      setFormError(getErrorMessage(err, '优惠券保存失败'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="admin-panel">
      <form
        className="admin-toolbar"
        onSubmit={(event) => {
          event.preventDefault()
          void refresh(1)
        }}
      >
        <div className="admin-search">
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="按优惠券名称查询"
            value={keyword}
          />
          <button type="submit">查询</button>
        </div>
        <button onClick={openCreateModal} type="button">
          新建优惠券
        </button>
      </form>

      <div className="data-table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>优惠券</th>
              <th>库存</th>
              <th>领取时间</th>
              <th>过期时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {batches.map((batch) => (
              <tr key={batch.id}>
                <td>
                  <strong>{batch.name}</strong>
                  <span>{batch.description || '暂无说明'}</span>
                </td>
                <td>{batch.claimedCount}/{batch.stock}</td>
                <td>{new Date(batch.claimStartTime).toLocaleString()} - {new Date(batch.claimEndTime).toLocaleString()}</td>
                <td>{new Date(batch.expireTime).toLocaleString()}</td>
                <td>{statusNames[batch.status]}</td>
                <td>
                  <button className="secondary-button" onClick={() => openEditModal(batch)} type="button">
                    编辑
                  </button>
                </td>
              </tr>
            ))}
            {!batches.length && (
              <tr>
                <td colSpan={6}>暂无优惠券批次</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination">
        <button disabled={page <= 1} onClick={() => void refresh(page - 1)} type="button">
          上一页
        </button>
        <span>第 {page} / {Math.max(Math.ceil(total / 10), 1)} 页</span>
        <button disabled={page >= Math.ceil(total / 10)} onClick={() => void refresh(page + 1)} type="button">
          下一页
        </button>
      </div>

      {modalOpen && (
        <div className="modal-backdrop" role="presentation">
          <form className="modal-panel activity-modal" onSubmit={handleSubmit}>
            <div className="modal-header">
              <h2>{editingBatch ? '编辑优惠券批次' : '新建优惠券批次'}</h2>
              <button className="icon-button" onClick={() => setModalOpen(false)} type="button">x</button>
            </div>
            <div className="form-grid">
              <label>
                名称
                <input onChange={(event) => setForm({ ...form, name: event.target.value })} required value={form.name} />
              </label>
              <label>
                类型
                <input onChange={(event) => setForm({ ...form, couponType: event.target.value })} required value={form.couponType} />
              </label>
              <label>
                权益内容
                <input onChange={(event) => setForm({ ...form, benefitText: event.target.value })} placeholder="如免费门票、周边兑换、报名优先权" required value={form.benefitText} />
              </label>
              <label>
                库存
                <input min={1} onChange={(event) => setForm({ ...form, stock: Number(event.target.value) })} required type="number" value={form.stock} />
              </label>
              <label>
                领取开始
                <input onChange={(event) => setForm({ ...form, claimStartTime: event.target.value })} required type="datetime-local" value={form.claimStartTime} />
              </label>
              <label>
                领取结束
                <input onChange={(event) => setForm({ ...form, claimEndTime: event.target.value })} required type="datetime-local" value={form.claimEndTime} />
              </label>
              <label>
                过期时间
                <input onChange={(event) => setForm({ ...form, expireTime: event.target.value })} required type="datetime-local" value={form.expireTime} />
              </label>
              <div className="role-checks">
                <span>可领取角色</span>
                {['REGISTERED_USER', 'CLUB_MEMBER', 'DEPARTMENT_LEADER', 'PRESIDENT'].map((role) => (
                  <label className="checkbox-row" key={role}>
                    <input
                      checked={form.allowedRoleCodes.includes(role)}
                      onChange={() => toggleRole(role)}
                      type="checkbox"
                    />
                    {role}
                  </label>
                ))}
              </div>
              <label className="form-grid-wide">
                说明
                <textarea onChange={(event) => setForm({ ...form, description: event.target.value })} rows={4} value={form.description} />
              </label>
            </div>
            {formError && <p className="form-error">{formError}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setModalOpen(false)} type="button">取消</button>
              <button disabled={saving} type="submit">{saving ? '保存中...' : '保存'}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}
