export default function PageLoading({ className = 'content' }: { className?: string }) {
  return (
    <section className={className}>
      <div className="page-loading" aria-label="正在加载">
        <span />
      </div>
    </section>
  )
}
