"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getCategories } from "@/services/api/categoryApi";
import { getCostumes } from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { useAuth } from "@/context/AuthContext";

const priceFormat = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const columns = [
  { key: "name", label: "Costume", render: (row) => <Link className="text-link" href={`/admin/costumes/${row.id}`}>{row.name}</Link> },
  { key: "sku", label: "SKU" },
  { key: "categoryName", label: "Category" },
  {
    key: "basePrice",
    label: "Base price",
    render: (row) => row.basePrice == null ? "—" : priceFormat.format(Number(row.basePrice)),
  },
  {
    key: "primaryImageUrl",
    label: "Primary image",
    render: (row) => row.primaryImageUrl
      ? <a className="text-link" href={row.primaryImageUrl} target="_blank" rel="noreferrer">View image ↗</a>
      : <span className="muted">—</span>,
  },
  { key: "status", label: "Status", render: () => <span className="status-pill status-active">Active</span> },
];

export default function CostumesPage() {
  const { can } = useAuth();
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [categories, setCategories] = useState([]);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [categoryError, setCategoryError] = useState("");
  const canViewCategories = can("CATEGORY_VIEW");

  useEffect(() => {
    if (!canViewCategories) return undefined;
    const controller = new AbortController();
    setCategoryError("");
    getCategories({ page: 0, size: 100, signal: controller.signal })
      .then((result) => setCategories(result.content || []))
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setCategories([]);
          setCategoryError(getApiErrorMessage(requestError, "Category filter options could not be loaded."));
        }
      });
    return () => controller.abort();
  }, [canViewCategories]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getCostumes({ page, size: 10, search: query, categoryId, signal: controller.signal })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load costumes."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query, categoryId]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  return (
    <ProtectedRoute permission="COSTUME_VIEW">
      <PageHeader
        eyebrow="CATALOG"
        title="Costumes"
        description="Browse active costume records and their published catalog details."
      />
      <section className="content-card">
        <div className="table-toolbar">
          <form className="search-form" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="costume-search">Search costumes</label>
            <input id="costume-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search costume names" maxLength={100} />
            <button className="button button-primary" type="submit">Search</button>
          </form>
          <span className="table-count">{pageData.totalElements} active costumes</span>
        </div>
        {canViewCategories && (
          <>
            <div className="filter-row">
              <label htmlFor="costume-category">Category</label>
              <select
                id="costume-category"
                className="form-control category-filter"
                value={categoryId}
                onChange={(event) => {
                  setPage(0);
                  setCategoryId(event.target.value);
                }}
              >
                <option value="">All categories</option>
                {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
              </select>
            </div>
            {categoryError && <p className="inline-error filter-error" role="alert">{categoryError}</p>}
          </>
        )}
        <DataTable
          columns={columns}
          rows={pageData.content}
          loading={loading}
          error={error}
          emptyTitle="No active costumes found"
          emptyDescription="Try a different search. Costume create, edit, and status changes need backend write APIs."
        />
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onChange={setPage}
        />
      </section>
      <p className="capability-note">Read-only: costume create, edit, activate/deactivate, and image upload APIs are not implemented. Category filtering uses active categories and requires CATEGORY_VIEW. Prices are displayed without a currency because the API does not provide one.</p>
    </ProtectedRoute>
  );
}
