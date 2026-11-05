export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalPages) return null;
  return (
    <div className="pagination">
      <span>{totalElements} records · Page {page + 1} of {totalPages}</span>
      <div className="pagination-actions">
        <button
          className="button button-small button-secondary"
          disabled={page <= 0}
          onClick={() => onChange(page - 1)}
        >
          Previous
        </button>
        <button
          className="button button-small button-secondary"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
