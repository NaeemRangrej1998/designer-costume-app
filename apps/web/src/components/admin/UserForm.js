"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getAdminUser, updateAdminUser } from "@/services/api/userApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function UserForm({ userId }) {
  const router = useRouter();
  const [form, setForm] = useState({ fullName: "", email: "", phoneNumber: "", roles: "", active: true });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getAdminUser(userId, controller.signal)
      .then((user) => setForm({
        fullName: user.fullName || "",
        email: user.email || "",
        phoneNumber: user.phoneNumber || "",
        roles: [...(user.roles || [])].join(", "),
        active: user.active,
      }))
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "User could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [userId]);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const roles = [...new Set(form.roles.split(",").map((role) => role.trim()).filter(Boolean))];
      await updateAdminUser(userId, {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.trim() || null,
        roles,
        active: form.active,
      });
      router.push("/admin/users");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "User could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  function updateField(event) {
    const { name, value, checked, type } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  return (
    <ProtectedRoute permission="USER_UPDATE">
      <PageHeader eyebrow="ACCESS CONTROL" title="Edit user" description="Role assignments are validated against your own effective permissions." />
      {loading ? <div className="inline-state"><span className="spinner" />Loading user…</div> : (
        <section className="content-card form-card">
          <form className="admin-form" onSubmit={submit}>
            <div className="form-grid">
              <label className="form-field"><span>Full name *</span><input className="form-control" name="fullName" value={form.fullName} onChange={updateField} maxLength={150} required /></label>
              <label className="form-field"><span>Email *</span><input className="form-control" name="email" type="email" value={form.email} onChange={updateField} maxLength={320} required /></label>
              <label className="form-field"><span>Phone number</span><input className="form-control" name="phoneNumber" value={form.phoneNumber} onChange={updateField} maxLength={32} /></label>
              <label className="form-field"><span>Roles *</span><input className="form-control" name="roles" value={form.roles} onChange={updateField} required /><small>Comma-separated role names. The backend checks that you can grant each role.</small></label>
              <label className="form-field checkbox-field"><input name="active" type="checkbox" checked={form.active} onChange={updateField} /><span>Active</span></label>
            </div>
            {error && <p className="inline-error" role="alert">{error}</p>}
            <div className="form-actions"><Link className="button button-secondary" href="/admin/users">Cancel</Link><button className="button button-primary" type="submit" disabled={saving}>{saving ? "Saving…" : "Save user"}</button></div>
          </form>
        </section>
      )}
    </ProtectedRoute>
  );
}
