import EmptyState from "./EmptyState";
import LoadingState from "./LoadingState";

export default function DataTable({ columns, rows, rowKey = "id", loading, error, emptyTitle, emptyDescription }) {
  if (loading) return <LoadingState label="Loading records…" />;
  if (error) return <div className="inline-error" role="alert">{error}</div>;
  if (!rows.length) {
    return <EmptyState title={emptyTitle || "Nothing here yet"} description={emptyDescription || "No records matched this view."} />;
  }

  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row[rowKey]}>
              {columns.map((column) => (
                <td key={column.key}>
                  {column.render ? column.render(row) : row[column.key] || <span className="muted">—</span>}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
