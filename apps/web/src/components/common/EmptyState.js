export default function EmptyState({ title, description }) {
  return (
    <div className="empty-state">
      <span className="empty-state-mark">—</span>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}
