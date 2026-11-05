"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getCostume, getCostumeVariants } from "@/services/api/costumeApi";

const priceFormat = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const variantColumns = [
  {
    key: "colorName",
    label: "Color",
    render: (variant) => (
      <span className="color-cell">
        <span className="color-swatch" style={{ backgroundColor: variant.colorHexCode || "#e5e2dc" }} />
        {variant.colorName}
        <span className="muted">{variant.colorHexCode}</span>
      </span>
    ),
  },
  { key: "sizeName", label: "Size" },
  { key: "price", label: "Price · currency unspecified", render: (variant) => priceFormat.format(Number(variant.price)) },
  { key: "available", label: "Availability", render: () => <span className="status-pill status-active">Available</span> },
];

export default function CostumeDetailPage() {
  const { id } = useParams();
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      getCostume(id, controller.signal),
      getCostumeVariants(id, controller.signal),
    ])
      .then(([costumeData, variantData]) => {
        setCostume(costumeData);
        setVariants(variantData);
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load costume details."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id]);

  return (
    <ProtectedRoute permission="COSTUME_VIEW">
      {loading ? (
        <div className="inline-state"><span className="spinner" />Loading costume details…</div>
      ) : error ? (
        <section className="content-card inline-error" role="alert">{error}</section>
      ) : costume ? (
        <>
          <PageHeader
            eyebrow="CATALOG · COSTUME"
            title={costume.name}
            description={costume.description || "No description is available for this costume."}
            action={<Link className="button button-secondary" href="/admin/costumes">Back to costumes</Link>}
          />
          <section className="content-card costume-summary">
            <div className="costume-summary-main">
              <span className="eyebrow">COSTUME DETAILS</span>
              <dl className="detail-grid">
                <div><dt>SKU</dt><dd>{costume.sku || "—"}</dd></div>
                <div><dt>Category</dt><dd>{costume.categoryName || "—"}</dd></div>
                <div><dt>Base price · currency unspecified</dt><dd>{costume.basePrice == null ? "—" : priceFormat.format(Number(costume.basePrice))}</dd></div>
                <div><dt>Status</dt><dd><span className="status-pill status-active">Active</span></dd></div>
              </dl>
            </div>
            {costume.primaryImageUrl && (
              <a className="costume-image-link" href={costume.primaryImageUrl} target="_blank" rel="noreferrer">
                View primary image <span aria-hidden="true">↗</span>
              </a>
            )}
          </section>
          <section className="content-card variants-card">
            <div className="section-heading">
              <div><p className="eyebrow">INVENTORY SNAPSHOT</p><h2>Available variants</h2></div>
              <span className="table-count">{variants.length} available</span>
            </div>
            <DataTable
              columns={variantColumns}
              rows={variants}
              emptyTitle="No available variants"
              emptyDescription="Unavailable variants are not returned by the existing API."
            />
          </section>
          <p className="capability-note">This view uses the existing active-costume and available-variants read APIs. Inventory changes are not supported.</p>
        </>
      ) : null}
    </ProtectedRoute>
  );
}
