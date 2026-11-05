"use client";

import { useEffect, useState } from "react";
import CategoryCard from "@/components/storefront/CategoryCard";
import StorePageHeading from "@/components/storefront/StorePageHeading";
import { StoreEmpty, StoreError, StoreLoading } from "@/components/storefront/StoreStates";
import { getCategories } from "@/services/api/categoryApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function CategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getCategories({ page: 0, size: 100, signal: controller.signal })
      .then((data) => setCategories(data.content || []))
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Categories are temporarily unavailable."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  return (
    <div className="store-page-container">
      <StorePageHeading eyebrow="THE COLLECTIONS" title="Explore by occasion" description="Browse the active categories curated by the boutique." />
      {loading ? <StoreLoading label="Loading categories…" /> :
        error ? <StoreError message={error} onRetry={() => window.location.reload()} /> :
          categories.length ? <div className="store-category-grid store-category-grid-page">{categories.map((category) => <CategoryCard category={category} key={category.id} />)}</div> :
            <StoreEmpty title="No categories available" message="There are no active categories to browse yet." />}
    </div>
  );
}
