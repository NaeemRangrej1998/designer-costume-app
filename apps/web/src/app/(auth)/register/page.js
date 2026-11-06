"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getApiErrorMessage } from "@/services/api/apiClient";

function isValidPassword(password) {
  const byteLength = new TextEncoder().encode(password).length;
  return password.length >= 8 && byteLength <= 72 &&
    /[A-Z]/.test(password) && /[a-z]/.test(password) &&
    /\d/.test(password) && /[^A-Za-z0-9\s]/.test(password);
}

export default function RegisterPage() {
  const router = useRouter();
  const { registerCustomer, status } = useAuth();
  const [values, setValues] = useState({ fullName: "", email: "", phoneNumber: "", password: "" });
  const [errors, setErrors] = useState({});
  const [requestError, setRequestError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") router.replace("/");
  }, [status, router]);

  function update(event) {
    setValues((current) => ({ ...current, [event.target.name]: event.target.value }));
    setErrors((current) => ({ ...current, [event.target.name]: "" }));
  }

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!values.fullName.trim()) nextErrors.fullName = "Enter your name.";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) nextErrors.email = "Enter a valid email address.";
    if (values.phoneNumber.length > 32) nextErrors.phoneNumber = "Phone number must be 32 characters or fewer.";
    if (!isValidPassword(values.password)) nextErrors.password = "Use 8–72 UTF-8 bytes with uppercase, lowercase, a number, and a special character.";
    setErrors(nextErrors);
    setRequestError("");
    if (Object.keys(nextErrors).length) return;

    setSubmitting(true);
    try {
      await registerCustomer({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phoneNumber: values.phoneNumber.trim() || null,
        password: values.password,
      });
      router.replace("/");
    } catch (error) {
      setRequestError(getApiErrorMessage(error, "Unable to create your account."));
    } finally {
      setSubmitting(false);
    }
  }

  if (status === "loading") {
    return <main className="login-page"><div className="login-card"><span className="spinner" />Checking your session…</div></main>;
  }

  return (
    <main className="login-page">
      <Link href="/" className="login-back">← Designer Costume Studio</Link>
      <section className="login-card register-card">
        <div className="brand-mark login-mark">DS</div>
        <p className="eyebrow">JOIN THE BOUTIQUE</p>
        <h1>Create an account</h1>
        <p className="login-intro">Create your customer account to prepare for future personalized experiences.</p>
        <form onSubmit={submit} className="login-form" noValidate>
          <label className="form-field"><span>Full name</span><input className="form-control" name="fullName" value={values.fullName} onChange={update} maxLength={150} autoComplete="name" required />{errors.fullName && <small className="field-error">{errors.fullName}</small>}</label>
          <label className="form-field"><span>Email address</span><input className="form-control" type="email" name="email" value={values.email} onChange={update} maxLength={320} autoComplete="email" required />{errors.email && <small className="field-error">{errors.email}</small>}</label>
          <label className="form-field"><span>Phone number <small>(optional)</small></span><input className="form-control" name="phoneNumber" value={values.phoneNumber} onChange={update} maxLength={32} autoComplete="tel" />{errors.phoneNumber && <small className="field-error">{errors.phoneNumber}</small>}</label>
          <label className="form-field"><span>Password</span><input className="form-control" type="password" name="password" value={values.password} onChange={update} autoComplete="new-password" required /><small>8–72 UTF-8 bytes; uppercase, lowercase, number, and special character.</small>{errors.password && <small className="field-error">{errors.password}</small>}</label>
          {requestError && <p className="inline-error" role="alert">{requestError}</p>}
          <button className="button button-primary login-submit" type="submit" disabled={submitting}>{submitting ? "Creating account…" : "Create customer account"}</button>
        </form>
        <p className="login-register-link">Already have an account? <Link href="/login">Sign in</Link></p>
        <p className="login-security">Registration creates a CUSTOMER account through the backend. Passwords are never stored in browser storage.</p>
      </section>
    </main>
  );
}
