export function LoadingState({ label = 'Đang tải dữ liệu…' }: { label?: string }) {
  return (
    <div className="state-card" role="status">
      <span className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}
