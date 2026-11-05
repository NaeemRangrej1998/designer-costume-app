"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getCategories } from "@/services/api/categoryApi";
import { createCostume, getAdminCostume, updateCostume } from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

const EMPTY_FORM = {
  name: "",
  description: "",
  sku: "",
  categoryId: "",
  basePrice: "",
  primaryImageUrl: "",
  active: true,
};

export default function CostumeForm({ costumeId }) {
  const router = useRouter();
  const [form, setForm] = useState(EMPTY_FORM);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(Boolean(costumeId));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      getCategories({ page: 0, size: 100, signal: controller.signal }),
      costumeId ? getAdminCostume(costumeId, controller.signal) : Promise.resolve(null),
    ])
      .then(([categoryPage, costume]) => {
        setCategories(categoryPage.content || []);
        if (costume) {
          setForm({
            name: costume.name || "",
            description: costume.description || "",
            sku: costume.sku || "",
            categoryId: costume.categoryId || "",
            basePrice: costume.basePrice == null ? "" : String(costume.basePrice),
            primaryImageUrl: costume.primaryImageUrl || "",
            active: costume.active,
          });
        }
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Costume data could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [costumeId]);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload = {
        ...form,
        name: form.name.trim(),
        description: form.description.trim() || null,
        sku: form.sku.trim(),
        basePrice: Number(form.basePrice),
        primaryImageUrl: form.primaryImageUrl.trim() || null,
      };
      if (costumeId) await updateCostume(costumeId, payload);
      else await createCostume(payload);
      router.push("/admin/costumes");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Costume could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  function updateField(event) {
    const { name, value, checked, type } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  return (
    <ProtectedRoute permission={costumeId ? "COSTUME_UPDATE" : "COSTUME_CREATE"}>
      <PageHeader
        eyebrow="CATALOG"
        title={costumeId ? "Edit costume" : "Create costume"}
        description="Manage the catalog record and its primary image URL."
      />
      {loading ? <div className="inline-state"><span className="spinner" />Loading costume…</div> : (
        <section className="content-card form-card">
          <form className="admin-form" onSubmit={submit}>
            <div className="form-grid">
              <label className="form-field">
                <span>Name <b>*</b></span>
                <input className="form-control" name="name" value={form.name} onChange={updateField} maxLength={200} required />
              </label>
              <label className="form-field">
                <span>SKU <b>*</b></span>
                <input className="form-control" name="sku" value={form.sku} onChange={updateField} maxLength={80} required />
              </label>
              <label className="form-field">
                <span>Category <b>*</b></span>
                <select className="form-control" name="categoryId" value={form.categoryId} onChange={updateField} required>
                  <option value="">Select category</option>
                  {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
                </select>
              </label>
              <label className="form-field">
                <span>Base price <b>*</b></span>
                <input className="form-control" name="basePrice" type="number" min="0" step="0.01" value={form.basePrice} onChange={updateField} required />
              </label>
              <label className="form-field form-field-wide">
                <span>Description</span>
                <textarea className="form-control" name="description" value={form.description} onChange={updateField} maxLength={5000} rows={4} />
              </label>
              <label className="form-field form-field-wide">
                <span>Primary image URL</span>
                <input className="form-control" name="primaryImageUrl" type="url" value={form.primaryImageUrl} onChange={updateField} maxLength={2048} />
                <small>Enter an HTTPS image URL. File uploads are not configured.</small>
              </label>
              <label className="form-field checkbox-field">
                <input name="active" type="checkbox" checked={form.active} onChange={updateField} />
                <span>Active</span>
              </label>
            </div>
            {error && <p className="inline-error" role="alert">{error}</p>}
            <div className="form-actions">
              <Link className="button button-secondary" href="/admin/costumes">Cancel</Link>
              <button className="button button-primary" type="submit" disabled={saving || loading}>
                {saving ? "Saving…" : "Save costume"}
              </button>
            </div>
          </form>
        </section>
      )}
    </ProtectedRoute>
  );
}
