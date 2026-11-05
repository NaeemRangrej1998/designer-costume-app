"use client";

import { useEffect, useState } from "react";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getCategories } from "@/services/api/categoryApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

const columns = [
  {
    key: "imageUrl",
    label: "Image",
    render: (category) => category.imageUrl
      ? <a className="category-image-preview" href={category.imageUrl} target="_blank" rel="noreferrer" aria-label={`Open ${category.name} image`} style={{ backgroundImage: `url("${category.imageUrl}")` }} />
      : <span className="muted">—</span>,
  },
  { key: "name", label: "Category" },
  { key: "description", label: "Description" },
  { key: "parentCategoryName", label: "Parent category" },
  { key: "status", label: "Status", render: () => <span className="status-pill status-active">Active</span> },
];

export default function CategoriesPage() {
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getCategories({ page, size: 10, search: query, signal: controller.signal })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load categories."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  return (
    <ProtectedRoute permission="CATEGORY_VIEW">
      <PageHeader
        eyebrow="CATALOG"
        title="Categories"
        description="Browse active categories available in the catalog."
      />
      <section className="content-card">
        <div className="table-toolbar">
          <form className="search-form" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="category-search">Search categories</label>
            <input id="category-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search category names" maxLength={100} />
            <button className="button button-primary" type="submit">Search</button>
          </form>
          <span className="table-count">{pageData.totalElements} active categories</span>
        </div>
        <DataTable
          columns={columns}
          rows={pageData.content}
          loading={loading}
          error={error}
          emptyTitle="No active categories found"
          emptyDescription="Try a different search. Creating and editing categories is not available because the backend has no write endpoints."
        />
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onChange={setPage}
        />
      </section>
      <p className="capability-note">Read-only: category create, edit, activate/deactivate, image, and parent assignment APIs are not implemented.</p>
    </ProtectedRoute>
  );
}
