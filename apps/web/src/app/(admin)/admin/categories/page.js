"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { deactivateCategory, getAdminCategories } from "@/services/api/categoryApi";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { useAuth } from "@/context/AuthContext";

const baseColumns = [
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
  { key: "active", label: "Status", render: (category) => <span className={`status-pill ${category.active ? "status-active" : "status-inactive"}`}>{category.active ? "Active" : "Inactive"}</span> },
];

export default function CategoriesPage() {
  const { can } = useAuth();
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("true");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getAdminCategories({
      page,
      size: 10,
      search: query,
      active: activeFilter === "" ? undefined : activeFilter === "true",
      signal: controller.signal,
    })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load categories."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query, activeFilter]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  async function handleDeactivate(category) {
    if (!window.confirm(`Deactivate ${category.name}?`)) return;
    setActionError("");
    try {
      await deactivateCategory(category.id);
      setPageData((current) => ({
        ...current,
        content: current.content.filter((item) => item.id !== category.id),
        totalElements: Math.max(0, current.totalElements - 1),
      }));
    } catch (requestError) {
      setActionError(getApiErrorMessage(requestError, "Category could not be deactivated."));
    }
  }

  const columns = [
    ...baseColumns,
    {
      key: "actions",
      label: "Actions",
      render: (category) => (
        <div className="table-actions">
          {can("CATEGORY_UPDATE") && <Link className="text-link" href={`/admin/categories/${category.id}/edit`}>Edit</Link>}
          {category.active && can("CATEGORY_DELETE") && (
            <button className="text-button text-danger" type="button" onClick={() => handleDeactivate(category)}>Deactivate</button>
          )}
        </div>
      ),
    },
  ];

  return (
    <ProtectedRoute permission="CATEGORY_VIEW">
      <PageHeader
        eyebrow="CATALOG"
        title="Categories"
        description="Manage categories available in the catalog."
        action={can("CATEGORY_CREATE") && <Link className="button button-primary" href="/admin/categories/new">Create category</Link>}
      />
      <section className="content-card">
        <div className="table-toolbar">
          <form className="search-form" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="category-search">Search categories</label>
            <input id="category-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search category names" maxLength={100} />
            <button className="button button-primary" type="submit">Search</button>
          </form>
          <label className="filter-control">
            <span>Status</span>
            <select className="form-control" value={activeFilter} onChange={(event) => { setPage(0); setActiveFilter(event.target.value); }}>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
              <option value="">All</option>
            </select>
          </label>
          <span className="table-count">{pageData.totalElements} categories</span>
        </div>
        {actionError && <p className="inline-error" role="alert">{actionError}</p>}
        <DataTable
          columns={columns}
          rows={pageData.content}
          loading={loading}
          error={error}
          emptyTitle="No categories found"
          emptyDescription="Try a different search or status filter."
        />
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onChange={setPage}
        />
      </section>
    </ProtectedRoute>
  );
}
