"use client";

import { useEffect, useState } from "react";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { useAuth } from "@/context/AuthContext";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getColors, getSizes, saveColor, saveSize } from "@/services/api/costumeApi";

const EMPTY_COLOR = { id: "", name: "", hexCode: "#000000", active: true };
const EMPTY_SIZE = { id: "", name: "", displayOrder: 0, active: true };

export default function CatalogOptionsPage() {
  const { can } = useAuth();
  const [colors, setColors] = useState([]);
  const [sizes, setSizes] = useState([]);
  const [colorForm, setColorForm] = useState(EMPTY_COLOR);
  const [sizeForm, setSizeForm] = useState(EMPTY_SIZE);
  const [loading, setLoading] = useState(true);
  const [savingColor, setSavingColor] = useState(false);
  const [savingSize, setSavingSize] = useState(false);
  const [error, setError] = useState("");

  async function refresh() {
    const [colorData, sizeData] = await Promise.all([getColors(), getSizes()]);
    setColors(colorData);
    setSizes(sizeData);
  }

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([getColors(controller.signal), getSizes(controller.signal)])
      .then(([colorData, sizeData]) => { setColors(colorData); setSizes(sizeData); })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Catalog options could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  async function submitColor(event) {
    event.preventDefault();
    setSavingColor(true);
    setError("");
    try {
      await saveColor(colorForm.id || null, { ...colorForm, name: colorForm.name.trim(), hexCode: colorForm.hexCode.toUpperCase() });
      setColorForm(EMPTY_COLOR);
      await refresh();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Color could not be saved."));
    } finally {
      setSavingColor(false);
    }
  }

  async function submitSize(event) {
    event.preventDefault();
    setSavingSize(true);
    setError("");
    try {
      await saveSize(sizeForm.id || null, { ...sizeForm, name: sizeForm.name.trim(), displayOrder: Number(sizeForm.displayOrder) });
      setSizeForm(EMPTY_SIZE);
      await refresh();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Size could not be saved."));
    } finally {
      setSavingSize(false);
    }
  }

  const colorColumns = [
    { key: "name", label: "Color" },
    { key: "hexCode", label: "Hex", render: (color) => <span className="color-cell"><span className="color-swatch" style={{ backgroundColor: color.hexCode }} />{color.hexCode}</span> },
    { key: "active", label: "Status", render: (color) => color.active ? "Active" : "Inactive" },
    { key: "actions", label: "Actions", render: (color) => can("COSTUME_UPDATE") && <button className="text-button" type="button" onClick={() => setColorForm(color)}>Edit</button> },
  ];
  const sizeColumns = [
    { key: "name", label: "Size" },
    { key: "displayOrder", label: "Display order" },
    { key: "active", label: "Status", render: (size) => size.active ? "Active" : "Inactive" },
    { key: "actions", label: "Actions", render: (size) => can("COSTUME_UPDATE") && <button className="text-button" type="button" onClick={() => setSizeForm(size)}>Edit</button> },
  ];

  return (
    <ProtectedRoute permission="COSTUME_VIEW">
      <PageHeader eyebrow="CATALOG" title="Colors & sizes" description="Maintain the options used by costume variants." />
      {error && <p className="inline-error" role="alert">{error}</p>}
      {loading ? <div className="inline-state"><span className="spinner" />Loading catalog options…</div> : (
        <div className="catalog-option-grid">
          <section className="content-card">
            <div className="section-heading"><h2>Colors</h2><span className="table-count">{colors.length} records</span></div>
            <DataTable columns={colorColumns} rows={colors} emptyTitle="No colors" emptyDescription="Create a color to use it for costume variants." />
            {(can("COSTUME_CREATE") || can("COSTUME_UPDATE")) && (
              <form className="admin-form compact-form" onSubmit={submitColor}>
                <h3>{colorForm.id ? "Edit color" : "Add color"}</h3>
                <label className="form-field"><span>Name *</span><input className="form-control" value={colorForm.name} onChange={(event) => setColorForm((current) => ({ ...current, name: event.target.value }))} maxLength={80} required /></label>
                <label className="form-field"><span>Hex code *</span><input className="form-control" value={colorForm.hexCode} onChange={(event) => setColorForm((current) => ({ ...current, hexCode: event.target.value }))} pattern="^#[0-9A-Fa-f]{6}$" required /></label>
                <label className="form-field checkbox-field"><input type="checkbox" checked={colorForm.active} onChange={(event) => setColorForm((current) => ({ ...current, active: event.target.checked }))} /><span>Active</span></label>
                <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setColorForm(EMPTY_COLOR)}>Clear</button><button className="button button-primary" type="submit" disabled={savingColor || (!colorForm.id && !can("COSTUME_CREATE")) || (colorForm.id && !can("COSTUME_UPDATE"))}>{savingColor ? "Saving…" : "Save color"}</button></div>
              </form>
            )}
          </section>
          <section className="content-card">
            <div className="section-heading"><h2>Sizes</h2><span className="table-count">{sizes.length} records</span></div>
            <DataTable columns={sizeColumns} rows={sizes} emptyTitle="No sizes" emptyDescription="Create a size to use it for costume variants." />
            {(can("COSTUME_CREATE") || can("COSTUME_UPDATE")) && (
              <form className="admin-form compact-form" onSubmit={submitSize}>
                <h3>{sizeForm.id ? "Edit size" : "Add size"}</h3>
                <label className="form-field"><span>Name *</span><input className="form-control" value={sizeForm.name} onChange={(event) => setSizeForm((current) => ({ ...current, name: event.target.value }))} maxLength={40} required /></label>
                <label className="form-field"><span>Display order</span><input className="form-control" type="number" value={sizeForm.displayOrder} onChange={(event) => setSizeForm((current) => ({ ...current, displayOrder: event.target.value }))} /></label>
                <label className="form-field checkbox-field"><input type="checkbox" checked={sizeForm.active} onChange={(event) => setSizeForm((current) => ({ ...current, active: event.target.checked }))} /><span>Active</span></label>
                <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setSizeForm(EMPTY_SIZE)}>Clear</button><button className="button button-primary" type="submit" disabled={savingSize || (!sizeForm.id && !can("COSTUME_CREATE")) || (sizeForm.id && !can("COSTUME_UPDATE"))}>{savingSize ? "Saving…" : "Save size"}</button></div>
              </form>
            )}
          </section>
        </div>
      )}
    </ProtectedRoute>
  );
}
