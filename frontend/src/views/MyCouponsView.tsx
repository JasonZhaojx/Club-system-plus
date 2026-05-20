import { useEffect, useMemo, useState, type UIEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  listMyCouponRedemptions,
  listMyCoupons,
  useCoupon,
  type CouponRedemption,
  type UserCoupon,
  type UserCouponStatus,
} from '@/api/modules/coupon'
import PageLoading from '@/components/PageLoading'
import { getErrorMessage, showToast } from '@/toast'

const statusNames: Record<UserCouponStatus, string> = {
  UNUSED: '未使用',
  USED: '已使用',
  EXPIRED: '已过期',
}

const couponBatchSize = 4
const redemptionBatchSize = 5

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function MyCouponsView() {
  const [coupons, setCoupons] = useState<UserCoupon[]>([])
  const [redemptions, setRedemptions] = useState<CouponRedemption[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [usingId, setUsingId] = useState<number | null>(null)
  const [visibleCoupons, setVisibleCoupons] = useState(couponBatchSize)
  const [visibleRedemptions, setVisibleRedemptions] = useState(redemptionBatchSize)

  const unusedCoupons = useMemo(
    () => coupons.filter((coupon) => coupon.status === 'UNUSED'),
    [coupons],
  )
  const couponRenderLimit = Math.min(coupons.length, visibleCoupons + (visibleCoupons < coupons.length ? 1 : 0))
  const redemptionRenderLimit = Math.min(
    redemptions.length,
    visibleRedemptions + (visibleRedemptions < redemptions.length ? 1 : 0),
  )
  const displayedCoupons = coupons.slice(0, couponRenderLimit)
  const displayedRedemptions = redemptions.slice(0, redemptionRenderLimit)

  useEffect(() => {
    void refresh()
  }, [])

  useEffect(() => {
    setVisibleCoupons(couponBatchSize)
  }, [coupons])

  useEffect(() => {
    setVisibleRedemptions(redemptionBatchSize)
  }, [redemptions])

  async function refresh() {
    setLoading(true)
    setError('')
    try {
      const [couponList, redemptionList] = await Promise.all([listMyCoupons(), listMyCouponRedemptions()])
      setCoupons(couponList)
      setRedemptions(redemptionList)
    } catch (err) {
      setError(getErrorMessage(err, '券包加载失败'))
    } finally {
      setLoading(false)
    }
  }

  async function handleUse(coupon: UserCoupon) {
    setUsingId(coupon.id)
    try {
      await useCoupon(coupon.id, { scene: 'manual', note: '用户主动核销' })
      showToast('优惠券已核销', 'success')
      await refresh()
    } catch (err) {
      showToast(getErrorMessage(err, '核销失败'))
    } finally {
      setUsingId(null)
    }
  }

  function handleCouponScroll(event: UIEvent<HTMLDivElement>) {
    const target = event.currentTarget
    const nearEnd = target.scrollLeft + target.clientWidth >= target.scrollWidth - 80
    if (nearEnd) {
      setVisibleCoupons((current) => Math.min(current + couponBatchSize, coupons.length))
    }
  }

  function handleRedemptionScroll(event: UIEvent<HTMLDivElement>) {
    const target = event.currentTarget
    const nearEnd = target.scrollTop + target.clientHeight >= target.scrollHeight - 40
    if (nearEnd) {
      setVisibleRedemptions((current) => Math.min(current + redemptionBatchSize, redemptions.length))
    }
  }

  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Wallet</p>
          <h1>我的券包</h1>
          <p>查看已领取优惠券、使用状态和核销记录。</p>
        </div>
        <Link className="back-button" to="/my">
          返回我的
        </Link>
      </div>

      {loading && <PageLoading className="inline-loading-section" />}
      {!loading && error && <div className="empty-state">{error}</div>}
      {!loading && !error && !coupons.length && <div className="empty-state">暂无优惠券</div>}

      <div className="wallet-coupon-strip" onScroll={handleCouponScroll}>
        {displayedCoupons.map((coupon) => (
          <section className="coupon-card" key={coupon.id}>
            <div>
              <span>{coupon.couponType}</span>
              <strong>{coupon.benefitText}</strong>
            </div>
            <h2>{coupon.batchName}</h2>
            <p>{coupon.description || '暂无说明'}</p>
            <dl>
              <div>
                <dt>状态</dt>
                <dd>{statusNames[coupon.status]}</dd>
              </div>
              <div>
                <dt>领取时间</dt>
                <dd>{formatDate(coupon.claimedAt)}</dd>
              </div>
              <div>
                <dt>过期时间</dt>
                <dd>{formatDate(coupon.expireTime)}</dd>
              </div>
            </dl>
            <button
              disabled={coupon.status !== 'UNUSED' || usingId === coupon.id}
              onClick={() => void handleUse(coupon)}
              type="button"
            >
              {usingId === coupon.id ? '核销中...' : coupon.status === 'UNUSED' ? '使用并核销' : statusNames[coupon.status]}
            </button>
          </section>
        ))}
      </div>

      {!!redemptions.length && (
        <section className="admin-panel coupon-history">
          <div className="section-heading">
            <h2>核销记录</h2>
          </div>
          <div className="data-table-wrap redemption-scroll" onScroll={handleRedemptionScroll}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>优惠券</th>
                  <th>场景</th>
                  <th>备注</th>
                  <th>核销时间</th>
                </tr>
              </thead>
              <tbody>
                {displayedRedemptions.map((record) => (
                  <tr key={record.id}>
                    <td>{record.batchName}</td>
                    <td>{record.scene || '-'}</td>
                    <td>{record.note || '-'}</td>
                    <td>{formatDate(record.redeemedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && !error && !!coupons.length && !unusedCoupons.length && (
        <div className="load-more-wrap">
          <span>当前没有可使用优惠券</span>
        </div>
      )}
    </section>
  )
}
