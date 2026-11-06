"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getCategories } from "@/services/api/categoryApi";
import { deactivateCostume, getAdminCostumes, getImageAssetUrl } from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { useAuth } from "@/context/AuthContext";

const priceFormat = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const baseColumns = [
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
      ? <a className="text-link" href={getImageAssetUrl(row.primaryImageUrl)} target="_blank" rel="noreferrer">View image ↗</a>
      : <span className="muted">—</span>,
  },
  { key: "active", label: "Status", render: (row) => <span className={`status-pill ${row.active ? "status-active" : "status-inactive"}`}>{row.active ? "Active" : "Inactive"}</span> },
];

export default function CostumesPage() {
  const { can } = useAuth();
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [categories, setCategories] = useState([]);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [activeFilter, setActiveFilter] = useState("true");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
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
    getAdminCostumes({
      page,
      size: 10,
      search: query,
      categoryId,
      active: activeFilter === "" ? undefined : activeFilter === "true",
      signal: controller.signal,
    })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load costumes."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query, categoryId, activeFilter]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  async function handleDeactivate(costume) {
    if (!window.confirm(`Deactivate ${costume.name}?`)) return;
    setActionError("");
    try {
      await deactivateCostume(costume.id);
      setPageData((current) => ({
        ...current,
        content: current.content.filter((item) => item.id !== costume.id),
        totalElements: Math.max(0, current.totalElements - 1),
      }));
    } catch (requestError) {
      setActionError(getApiErrorMessage(requestError, "Costume could not be deactivated."));
    }
  }

  const columns = [
    ...baseColumns,
    {
      key: "actions",
      label: "Actions",
      render: (row) => (
        <div className="table-actions">
          {can("COSTUME_UPDATE") && <Link className="text-link" href={`/admin/costumes/${row.id}/edit`}>Edit</Link>}
          {row.active && can("COSTUME_DELETE") && (
            <button className="text-button text-danger" type="button" onClick={() => handleDeactivate(row)}>Deactivate</button>
          )}
        </div>
      ),
    },
  ];

  return (
    <ProtectedRoute permission="COSTUME_VIEW">
      <PageHeader
        eyebrow="CATALOG"
        title="Costumes"
        description="Manage costume records and their published catalog details."
        action={can("COSTUME_CREATE") && <Link className="button button-primary" href="/admin/costumes/new">Create costume</Link>}
      />
      <section className="content-card">
        <div className="table-toolbar">
          <form className="search-form" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="costume-search">Search costumes</label>
            <input id="costume-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search costume names" maxLength={100} />
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
          <span className="table-count">{pageData.totalElements} costumes</span>
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
        {actionError && <p className="inline-error" role="alert">{actionError}</p>}
        <DataTable
          columns={columns}
          rows={pageData.content}
          loading={loading}
          error={error}
          emptyTitle="No costumes found"
          emptyDescription="Try a different search or status filter."
        />
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onChange={setPage}
        />
      </section>
      <p className="capability-note">Catalog images are managed by URL; file upload and currency selection are not configured. Category filtering uses active categories and requires CATEGORY_VIEW. Prices are displayed without a currency because the API does not provide one.</p>
    </ProtectedRoute>
  );
}
