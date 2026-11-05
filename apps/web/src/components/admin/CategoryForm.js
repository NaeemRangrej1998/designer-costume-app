"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getCategories } from "@/services/api/categoryApi";
import { createCategory, getAdminCategory, updateCategory } from "@/services/api/categoryApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

const EMPTY_FORM = {
  name: "",
  description: "",
  imageUrl: "",
  parentCategoryId: "",
  active: true,
};

export default function CategoryForm({ categoryId }) {
  const router = useRouter();
  const [form, setForm] = useState(EMPTY_FORM);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(Boolean(categoryId));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      getCategories({ page: 0, size: 100, signal: controller.signal }),
      categoryId ? getAdminCategory(categoryId, controller.signal) : Promise.resolve(null),
    ])
      .then(([categoryPage, category]) => {
        setCategories(categoryPage.content || []);
        if (category) {
          setForm({
            name: category.name || "",
            description: category.description || "",
            imageUrl: category.imageUrl || "",
            parentCategoryId: category.parentCategoryId || "",
            active: category.active,
          });
        }
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Category data could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [categoryId]);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const payload = {
        ...form,
        name: form.name.trim(),
        description: form.description.trim() || null,
        imageUrl: form.imageUrl.trim() || null,
        parentCategoryId: form.parentCategoryId || null,
      };
      if (categoryId) await updateCategory(categoryId, payload);
      else await createCategory(payload);
      router.push("/admin/categories");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Category could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  function updateField(event) {
    const { name, value, checked, type } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  return (
    <ProtectedRoute permission={categoryId ? "CATEGORY_UPDATE" : "CATEGORY_CREATE"}>
      <PageHeader
        eyebrow="CATALOG"
        title={categoryId ? "Edit category" : "Create category"}
        description="Manage category details and catalog visibility."
      />
      {loading ? <div className="inline-state"><span className="spinner" />Loading category…</div> : (
        <section className="content-card form-card">
          <form className="admin-form" onSubmit={submit}>
            <div className="form-grid">
              <label className="form-field">
                <span>Name <b>*</b></span>
                <input className="form-control" name="name" value={form.name} onChange={updateField} maxLength={150} required />
              </label>
              <label className="form-field">
                <span>Parent category</span>
                <select className="form-control" name="parentCategoryId" value={form.parentCategoryId} onChange={updateField}>
                  <option value="">No parent</option>
                  {categories.filter((category) => category.id !== categoryId).map((category) => (
                    <option value={category.id} key={category.id}>{category.name}</option>
                  ))}
                </select>
              </label>
              <label className="form-field form-field-wide">
                <span>Description</span>
                <textarea className="form-control" name="description" value={form.description} onChange={updateField} maxLength={2000} rows={4} />
              </label>
              <label className="form-field form-field-wide">
                <span>Image URL</span>
                <input className="form-control" name="imageUrl" type="url" value={form.imageUrl} onChange={updateField} maxLength={2048} />
                <small>Enter an HTTPS image URL. File uploads are not configured.</small>
              </label>
              <label className="form-field checkbox-field">
                <input name="active" type="checkbox" checked={form.active} onChange={updateField} />
                <span>Active</span>
              </label>
            </div>
            {error && <p className="inline-error" role="alert">{error}</p>}
            <div className="form-actions">
              <Link className="button button-secondary" href="/admin/categories">Cancel</Link>
              <button className="button button-primary" type="submit" disabled={saving || loading}>
                {saving ? "Saving…" : "Save category"}
              </button>
            </div>
          </form>
        </section>
      )}
    </ProtectedRoute>
  );
}
