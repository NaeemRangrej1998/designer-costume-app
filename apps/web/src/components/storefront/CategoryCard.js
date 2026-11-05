import Link from "next/link";

export default function CategoryCard({ category }) {
  return (
    <Link
      className="category-card"
      href={`/costumes?categoryId=${encodeURIComponent(category.id)}`}
      style={category.imageUrl ? { backgroundImage: `linear-gradient(180deg, transparent 30%, rgba(27,24,20,.75)), url("${category.imageUrl}")` } : undefined}
    >
      {!category.imageUrl && <span className="category-card-monogram">{category.name?.slice(0, 1)}</span>}
      <span className="category-card-copy">
        <span className="category-card-kicker">EXPLORE THE EDIT</span>
        <strong>{category.name}</strong>
        <span className="category-card-link">Discover collection <span>↗</span></span>
      </span>
    </Link>
  );
}
