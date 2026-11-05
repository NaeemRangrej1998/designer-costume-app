"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { createAdminUser } from "@/services/api/userApi";

function isValidPassword(password) {
  return password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password) &&
    /[^A-Za-z0-9\s]/.test(password) &&
    new TextEncoder().encode(password).length <= 72;
}

export default function CreateUserPage() {
  const router = useRouter();
  const [form, setForm] = useState({ fullName: "", email: "", phoneNumber: "", password: "", roles: "" });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState("");
  const [createdUser, setCreatedUser] = useState(null);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    setErrors((current) => ({ ...current, [event.target.name]: "" }));
  }

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!form.fullName.trim()) nextErrors.fullName = "Enter the user’s full name.";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) nextErrors.email = "Enter a valid email address.";
    if (!isValidPassword(form.password)) nextErrors.password = "Use 8–72 UTF-8 bytes with uppercase, lowercase, a number, and a special character.";
    const roles = [...new Set(form.roles.split(",").map((role) => role.trim()).filter(Boolean))];
    if (!roles.length) nextErrors.roles = "Enter at least one backend role name.";
    setErrors(nextErrors);
    setApiError("");
    setCreatedUser(null);
    if (Object.keys(nextErrors).length) return;

    setSaving(true);
    try {
      const newUser = await createAdminUser({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.trim() || null,
        password: form.password,
        roles,
      });
      setCreatedUser(newUser);
      setForm({ fullName: "", email: "", phoneNumber: "", password: "", roles: "" });
    } catch (error) {
      setApiError(getApiErrorMessage(error, "The user could not be created."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <ProtectedRoute permission="USER_CREATE">
      <PageHeader eyebrow="ACCESS CONTROL" title="Create user" description="The backend validates role assignments and permission limits." />
      <section className="content-card form-card">
        {createdUser && (
          <div className="success-message" role="status">
            <strong>User created successfully.</strong>
            <span>{createdUser.fullName} · {createdUser.email}</span>
            <Link href="/admin/users">View user administration status</Link>
          </div>
        )}
        <form className="admin-form" onSubmit={submit} noValidate>
          <div className="form-grid">
            <label className="form-field">
              <span>Full name <b>*</b></span>
              <input className="form-control" name="fullName" value={form.fullName} onChange={updateField} maxLength={150} autoComplete="name" required />
              {errors.fullName && <small className="field-error">{errors.fullName}</small>}
            </label>
            <label className="form-field">
              <span>Email <b>*</b></span>
              <input className="form-control" name="email" type="email" value={form.email} onChange={updateField} maxLength={320} autoComplete="email" required />
              {errors.email && <small className="field-error">{errors.email}</small>}
            </label>
            <label className="form-field">
              <span>Phone number</span>
              <input className="form-control" name="phoneNumber" value={form.phoneNumber} onChange={updateField} maxLength={32} autoComplete="tel" />
            </label>
            <label className="form-field">
              <span>Roles <b>*</b></span>
              <input className="form-control" name="roles" value={form.roles} onChange={updateField} placeholder="ADMIN, CUSTOMER" required />
              <small>Comma-separated role names. The server decides whether you may grant them.</small>
              {errors.roles && <small className="field-error">{errors.roles}</small>}
            </label>
            <label className="form-field form-field-wide">
              <span>Temporary password <b>*</b></span>
              <input className="form-control" name="password" type="password" value={form.password} onChange={updateField} autoComplete="new-password" required />
              <small>Use 8–72 UTF-8 bytes, including uppercase, lowercase, number, and special character.</small>
              {errors.password && <small className="field-error">{errors.password}</small>}
            </label>
          </div>
          {apiError && <p className="inline-error" role="alert">{apiError}</p>}
          <div className="form-actions">
            <button className="button button-secondary" type="button" onClick={() => router.push("/admin/users")}>Cancel</button>
            <button className="button button-primary" type="submit" disabled={saving}>{saving ? "Creating…" : "Create user"}</button>
          </div>
        </form>
      </section>
      <p className="capability-note">The password is sent only to the backend over the configured API connection and is never saved in browser storage.</p>
    </ProtectedRoute>
  );
}
