import { FormEvent, useEffect, useRef, useState } from 'react'
import { claimCoupon, listCouponBatches, listMyCoupons, type CouponBatch } from '@/api/modules/coupon'
import { getAccessToken } from '@/auth'
import { getErrorMessage, showToast } from '@/toast'

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

const batchSize = 8

export default function CouponsView() {
  const [batches, setBatches] = useState<CouponBatch[]>([])
  const [claimedBatchIds, setClaimedBatchIds] = useState<Set<number>>(new Set())
  const [keyword, setKeyword] = useState('')
  const [queryKeyword, setQueryKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [claimingId, setClaimingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const loadMoreRef = useRef<HTMLDivElement | null>(null)

  const hasMore = batches.length < total

  useEffect(() => {
    void refresh()
  }, [])

  useEffect(() => {
    const target = loadMoreRef.current
    if (!target || !hasMore || loading) {
      return
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          void loadBatches(page + 1, false)
        }
      },
      { rootMargin: '260px 0px' },
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [hasMore, loading, page, queryKeyword])

  async function refresh() {
    await loadBatches(1, true)
  }

  async function loadBatches(nextPage: number, replace: boolean, submittedKeyword = queryKeyword) {
    setLoading(true)
    setError('')
    try {
      const [result, myCoupons] = await Promise.all([
        listCouponBatches({ page: nextPage, size: batchSize, keyword: submittedKeyword || undefined }),
        getAccessToken() ? listMyCoupons().catch(() => []) : Promise.resolve([]),
      ])
      setBatches((current) => (replace ? result.records : [...current, ...result.records]))
      setPage(result.page)
      setTotal(result.total)
      setClaimedBatchIds(new Set(myCoupons.map((coupon) => coupon.batchId)))
    } catch (err) {
      setError(getErrorMessage(err, '优惠券加载失败'))
    } finally {
      setLoading(false)
    }
  }

  async function handleSearch(event: FormEvent) {
    event.preventDefault()
    const submittedKeyword = keyword.trim()
    setQueryKeyword(submittedKeyword)
    await loadBatches(1, true, submittedKeyword)
  }

  async function handleClaim(batch: CouponBatch) {
    setClaimingId(batch.id)
    try {
      await claimCoupon(batch.id)
      showToast('领取成功，已放入我的券包', 'success')
      setClaimedBatchIds((current) => new Set(current).add(batch.id))
      setBatches((current) =>
        current.map((item) =>
          item.id === batch.id
            ? { ...item, claimedCount: item.claimedCount + 1, remainingCount: Math.max(item.remainingCount - 1, 0) }
            : item,
        ),
      )
    } catch (err) {
      showToast(getErrorMessage(err, '领取失败'))
    } finally {
      setClaimingId(null)
    }
  }

  return (
    <section className="content public-page">
      <div className="section-head">
        <div>
          <p className="profile-eyebrow">Coupons</p>
          <h1>优惠券中心</h1>
          <p>领取社团活动、物料和服务相关优惠券。每个批次同一用户只能领取一次。</p>
        </div>
      </div>

      <form className="activity-tools coupon-tools" onSubmit={handleSearch}>
        <input
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索优惠券名称或说明"
          value={keyword}
        />
        <button type="submit">查询</button>
      </form>

      {loading && <div className="empty-state">正在加载优惠券...</div>}
      {!loading && error && <div className="empty-state">{error}</div>}
      {!loading && !error && !batches.length && <div className="empty-state">暂无可领取优惠券</div>}

      <div className="coupon-grid">
        {batches.map((batch) => (
          <section className="coupon-card" key={batch.id}>
            <div>
              <span>{batch.couponType}</span>
              <strong>{batch.benefitText}</strong>
            </div>
            <h2>{batch.name}</h2>
            <p>{batch.description || '暂无说明'}</p>
            <dl>
              <div>
                <dt>库存</dt>
                <dd>{batch.remainingCount}/{batch.stock}</dd>
              </div>
              <div>
                <dt>领取</dt>
                <dd>{formatDate(batch.claimStartTime)} - {formatDate(batch.claimEndTime)}</dd>
              </div>
              <div>
                <dt>过期</dt>
                <dd>{formatDate(batch.expireTime)}</dd>
              </div>
            </dl>
            <button
              disabled={claimedBatchIds.has(batch.id) || batch.remainingCount <= 0 || claimingId === batch.id}
              onClick={() => void handleClaim(batch)}
              type="button"
            >
              {claimedBatchIds.has(batch.id)
                ? '已领取'
                : claimingId === batch.id
                  ? '领取中...'
                  : batch.remainingCount > 0
                    ? '领取'
                    : '已领完'}
            </button>
          </section>
        ))}
      </div>

      <div className="load-more-wrap" ref={loadMoreRef}>
        {loading
          ? <span>正在加载优惠券...</span>
          : hasMore
            ? <span>继续向下浏览，自动加载更多优惠券</span>
            : batches.length
              ? <span>已展示全部优惠券</span>
              : null}
      </div>
    </section>
  )
}
