"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import CostumeGrid from "./CostumeGrid";
import StorePageHeading from "./StorePageHeading";
import StorePagination from "./StorePagination";
import { StoreEmpty, StoreError, StoreLoading } from "./StoreStates";
import { getCategories } from "@/services/api/categoryApi";
import { getCostumes } from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function CostumeListing() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [categoryError, setCategoryError] = useState("");
  const query = searchParams.get("search") || "";
  const [search, setSearch] = useState(query);
  const categoryId = searchParams.get("categoryId") || "";
  const page = Math.max(0, Number.parseInt(searchParams.get("page") || "0", 10) || 0);

  useEffect(() => setSearch(query), [query]);

  useEffect(() => {
    const controller = new AbortController();
    getCategories({ page: 0, size: 100, signal: controller.signal })
      .then((result) => setCategories(result.content || []))
      .catch((requestError) => {
        if (!controller.signal.aborted) setCategoryError(getApiErrorMessage(requestError, "Category filter options could not be loaded."));
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getCostumes({ page, size: 12, search: query, categoryId, signal: controller.signal })
      .then(setData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Costumes are temporarily unavailable."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query, categoryId]);

  const updateQuery = useCallback((updates) => {
    const params = new URLSearchParams(searchParams.toString());
    Object.entries(updates).forEach(([key, value]) => {
      if (value === "" || value == null || value === 0 && key === "page") params.delete(key);
      else params.set(key, String(value));
    });
    const queryString = params.toString();
    router.push(`/costumes${queryString ? `?${queryString}` : ""}`, { scroll: false });
  }, [router, searchParams]);

  useEffect(() => {
    if (search.trim() === query) return undefined;
    const timeoutId = window.setTimeout(() => {
      updateQuery({ search: search.trim(), page: 0 });
    }, 350);
    return () => window.clearTimeout(timeoutId);
  }, [search, query, updateQuery]);

  function submitSearch(event) {
    event.preventDefault();
    updateQuery({ search: event.currentTarget.elements.search.value.trim(), page: 0 });
  }

  const selectedCategory = categories.find((category) => category.id === categoryId);

  return (
    <div className="store-page-container">
      <StorePageHeading
        eyebrow="THE BOUTIQUE COLLECTION"
        title={selectedCategory?.name || "Designer costumes"}
        description="Discover active pieces in the current boutique catalog."
      />
      <section className="store-listing-toolbar">
        <form className="store-search-form" onSubmit={submitSearch}>
          <label className="store-visually-hidden" htmlFor="store-costume-search">Search costumes</label>
          <span aria-hidden="true">⌕</span>
          <input id="store-costume-search" name="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search the collection" maxLength={100} />
          <button type="submit">Search</button>
        </form>
        <label className="store-filter-label">
          <span>Occasion</span>
          <select
            value={categoryId}
            onChange={(event) => updateQuery({ categoryId: event.target.value, page: 0 })}
            aria-label="Filter by category"
          >
            <option value="">All collections</option>
            {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
          </select>
        </label>
      </section>
      {categoryError && <p className="store-inline-note" role="status">{categoryError}</p>}
      <div className="store-results-count">
        {loading ? "Updating the edit…" : `${data.totalElements} ${data.totalElements === 1 ? "piece" : "pieces"} in this edit`}
        <span>Sorted by name</span>
      </div>
      {loading ? <StoreLoading /> :
        error ? <StoreError message={error} onRetry={() => window.location.reload()} /> :
          data.content?.length ? <CostumeGrid costumes={data.content} /> :
            <StoreEmpty title="No pieces found" message="Try another search or choose a different collection." />}
      {!loading && !error && (
        <StorePagination page={data.page || 0} totalPages={data.totalPages || 0} onChange={(nextPage) => updateQuery({ page: nextPage })} />
      )}
    </div>
  );
}
