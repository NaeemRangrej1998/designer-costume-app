export default function StorePageHeading({ eyebrow, title, description }) {
  return (
    <header className="store-page-heading">
      <span className="store-eyebrow">{eyebrow}</span>
      <h1>{title}</h1>
      {description && <p>{description}</p>}
    </header>
  );
}
