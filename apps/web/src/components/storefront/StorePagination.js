export default function StorePagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="store-pagination" aria-label="Collection pages">
      <button className="store-button store-button-outline" onClick={() => onChange(page - 1)} disabled={page <= 0}>Previous</button>
      <span>{page + 1} <i>/</i> {totalPages}</span>
      <button className="store-button store-button-outline" onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>Next</button>
    </nav>
  );
}
