export function StoreLoading({ label = "Discovering the collection…" }) {
  return <div className="store-state" role="status"><span className="store-spinner" />{label}</div>;
}

export function StoreError({ message, onRetry }) {
  return (
    <div className="store-state store-state-error" role="alert">
      <h2>We couldn’t load this collection</h2>
      <p>{message}</p>
      {onRetry && <button className="store-button store-button-outline" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function StoreEmpty({ title, message }) {
  return <div className="store-state store-state-empty"><span className="store-empty-mark">✳</span><h2>{title}</h2><p>{message}</p></div>;
}
