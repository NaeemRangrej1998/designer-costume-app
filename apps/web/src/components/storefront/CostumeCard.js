import Link from "next/link";

function formatPrice(price) {
  if (price === null || price === undefined || price === "") return null;
  const value = Number(price);
  return Number.isFinite(value)
    ? new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)
    : null;
}

export default function CostumeCard({ costume }) {
  const price = formatPrice(costume.basePrice);
  return (
    <article className="costume-card">
      <Link className="costume-card-image" href={`/costumes/${costume.id}`} aria-label={`View ${costume.name}`}>
        {costume.primaryImageUrl
          ? <span className="costume-image-real" role="img" aria-label={costume.name} style={{ backgroundImage: `url("${costume.primaryImageUrl}")` }} />
          : <span className="costume-image-placeholder"><span>IMAGE<br />COMING SOON</span></span>}
        <span className="costume-card-badge">BOUTIQUE EDIT</span>
      </Link>
      <div className="costume-card-info">
        <div>
          <span className="costume-card-category">{costume.categoryName || "Designer occasionwear"}</span>
          <Link href={`/costumes/${costume.id}`} className="costume-card-title">{costume.name}</Link>
        </div>
        <div className="costume-card-price">{price ? <><span>{price}</span><small>Currency not specified</small></> : <span className="muted">Price on request</span>}</div>
      </div>
      <div className="costume-card-actions">
        <Link href={`/costumes/${costume.id}`}>View details <span>→</span></Link>
        <button type="button" disabled title="Favorites are unavailable until a backend favorites API exists" aria-label="Favorites unavailable">♡</button>
      </div>
    </article>
  );
}
