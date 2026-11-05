export default function LoadingState({ label = "Loading…" }) {
  return (
    <div className="inline-state" role="status">
      <span className="spinner" />
      {label}
    </div>
  );
}
