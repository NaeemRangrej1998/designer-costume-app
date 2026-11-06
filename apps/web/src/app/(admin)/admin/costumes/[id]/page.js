"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import CostumeImageGallery from "@/components/admin/CostumeImageGallery";
import { getApiErrorMessage } from "@/services/api/apiClient";
import {
  getImageAssetUrl,
  getAdminCostume,
  getAdminCostumeVariants,
  getColors,
  getSizes,
  saveCostumeVariant,
} from "@/services/api/costumeApi";
import { useAuth } from "@/context/AuthContext";

const priceFormat = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const baseVariantColumns = [
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
  { key: "sku", label: "SKU" },
  { key: "stockQuantity", label: "Stock" },
  { key: "available", label: "Availability", render: (variant) => <span className={`status-pill ${variant.available ? "status-active" : "status-inactive"}`}>{variant.available ? "Available" : "Unavailable"}</span> },
];

export default function CostumeDetailPage() {
  const { id } = useParams();
  const { can } = useAuth();
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [colors, setColors] = useState([]);
  const [sizes, setSizes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [variantForm, setVariantForm] = useState(null);
  const [variantSaving, setVariantSaving] = useState(false);
  const [variantError, setVariantError] = useState("");
  const [variantSuccess, setVariantSuccess] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      getAdminCostume(id, controller.signal),
      getAdminCostumeVariants(id, controller.signal),
      getColors(controller.signal),
      getSizes(controller.signal),
    ])
      .then(([costumeData, variantData, colorData, sizeData]) => {
        setCostume(costumeData);
        setVariants(variantData);
        setColors(colorData);
        setSizes(sizeData);
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load costume details."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id]);

  function startVariantEdit(variant = null) {
    setVariantError("");
    setVariantSuccess("");
    setVariantForm(variant ? {
      id: variant.id,
      colorId: variant.colorId,
      sizeId: variant.sizeId,
      sku: variant.sku,
      price: String(variant.price),
      stockQuantity: String(variant.stockQuantity),
      imageUrl: variant.imageUrl || "",
      available: variant.available,
    } : {
      id: "",
      colorId: colors.find((color) => color.active)?.id || "",
      sizeId: sizes.find((size) => size.active)?.id || "",
      sku: "",
      price: "",
      stockQuantity: "0",
      imageUrl: "",
      available: true,
    });
  }

  async function submitVariant(event) {
    event.preventDefault();
    setVariantSaving(true);
    setVariantError("");
    setVariantSuccess("");
    try {
      await saveCostumeVariant(id, variantForm.id || null, {
        colorId: variantForm.colorId,
        sizeId: variantForm.sizeId,
        sku: variantForm.sku.trim(),
        price: Number(variantForm.price),
        stockQuantity: Number(variantForm.stockQuantity),
        imageUrl: variantForm.imageUrl.trim() || null,
        available: variantForm.available,
      });
      setVariants(await getAdminCostumeVariants(id));
      setVariantForm(null);
      setVariantSuccess("Variant saved.");
    } catch (requestError) {
      setVariantError(getApiErrorMessage(requestError, "Variant could not be saved."));
    } finally {
      setVariantSaving(false);
    }
  }

  const variantColumns = [
    ...baseVariantColumns,
    ...(can("COSTUME_UPDATE") ? [{
      key: "actions",
      label: "Actions",
      render: (variant) => <button className="text-button" type="button" onClick={() => startVariantEdit(variant)}>Edit</button>,
    }] : []),
  ];

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
            action={(
              <div className="header-actions">
                {can("COSTUME_UPDATE") && <Link className="button button-primary" href={`/admin/costumes/${id}/edit`}>Edit costume</Link>}
                <Link className="button button-secondary" href="/admin/costumes">Back to costumes</Link>
              </div>
            )}
          />
          <section className="content-card costume-summary">
            <div className="costume-summary-main">
              <span className="eyebrow">COSTUME DETAILS</span>
              <dl className="detail-grid">
                <div><dt>SKU</dt><dd>{costume.sku || "—"}</dd></div>
                <div><dt>Category</dt><dd>{costume.categoryName || "—"}</dd></div>
                <div><dt>Base price · currency unspecified</dt><dd>{costume.basePrice == null ? "—" : priceFormat.format(Number(costume.basePrice))}</dd></div>
                <div><dt>Status</dt><dd><span className={`status-pill ${costume.active ? "status-active" : "status-inactive"}`}>{costume.active ? "Active" : "Inactive"}</span></dd></div>
              </dl>
            </div>
            {costume.primaryImageUrl && (
              <a className="costume-image-link" href={getImageAssetUrl(costume.primaryImageUrl)} target="_blank" rel="noreferrer">
                View primary image <span aria-hidden="true">↗</span>
              </a>
            )}
          </section>
          <CostumeImageGallery costumeId={id} canUpdate={can("COSTUME_UPDATE")} />
          <section className="content-card variants-card">
            <div className="section-heading">
              <div><p className="eyebrow">INVENTORY SNAPSHOT</p><h2>Available variants</h2></div>
              <div className="header-actions">
                <span className="table-count">{variants.filter((variant) => variant.available).length} available · {variants.length} total</span>
                {can("COSTUME_CREATE") && <button className="button button-small button-primary" type="button" onClick={() => startVariantEdit()}>Add variant</button>}
              </div>
            </div>
            <DataTable
              columns={variantColumns}
              rows={variants}
              emptyTitle="No variants"
              emptyDescription="Add a variant to manage stock and availability."
            />
            {variantSuccess && <p className="success-message" role="status">{variantSuccess}</p>}
            {variantForm && (
              <form className="admin-form variant-editor" onSubmit={submitVariant}>
                <h3>{variantForm.id ? "Edit variant" : "Add variant"}</h3>
                <div className="form-grid">
                  <label className="form-field"><span>Color *</span><select className="form-control" value={variantForm.colorId} onChange={(event) => setVariantForm((current) => ({ ...current, colorId: event.target.value }))} required><option value="">Select color</option>{colors.map((color) => <option key={color.id} value={color.id}>{color.name}{color.active ? "" : " (inactive)"}</option>)}</select></label>
                  <label className="form-field"><span>Size *</span><select className="form-control" value={variantForm.sizeId} onChange={(event) => setVariantForm((current) => ({ ...current, sizeId: event.target.value }))} required><option value="">Select size</option>{sizes.map((size) => <option key={size.id} value={size.id}>{size.name}{size.active ? "" : " (inactive)"}</option>)}</select></label>
                  <label className="form-field"><span>Variant SKU *</span><input className="form-control" value={variantForm.sku} onChange={(event) => setVariantForm((current) => ({ ...current, sku: event.target.value }))} maxLength={100} required /></label>
                  <label className="form-field"><span>Price *</span><input className="form-control" type="number" min="0" step="0.01" value={variantForm.price} onChange={(event) => setVariantForm((current) => ({ ...current, price: event.target.value }))} required /></label>
                  <label className="form-field"><span>Stock quantity *</span><input className="form-control" type="number" min="0" step="1" value={variantForm.stockQuantity} onChange={(event) => setVariantForm((current) => ({ ...current, stockQuantity: event.target.value }))} required /></label>
                  <label className="form-field"><span>Image URL</span><input className="form-control" type="url" value={variantForm.imageUrl} onChange={(event) => setVariantForm((current) => ({ ...current, imageUrl: event.target.value }))} maxLength={2048} /></label>
                  <label className="form-field checkbox-field"><input type="checkbox" checked={variantForm.available} onChange={(event) => setVariantForm((current) => ({ ...current, available: event.target.checked }))} /><span>Available</span></label>
                </div>
                {variantError && <p className="inline-error" role="alert">{variantError}</p>}
                <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setVariantForm(null)}>Cancel</button><button className="button button-primary" type="submit" disabled={variantSaving}>{variantSaving ? "Saving…" : "Save variant"}</button></div>
              </form>
            )}
          </section>
          <p className="capability-note">Catalog images are managed by URL; file upload and currency selection are not configured.</p>
        </>
      ) : null}
    </ProtectedRoute>
  );
}
