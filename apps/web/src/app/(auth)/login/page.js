"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { getApiErrorMessage } from "@/services/api/apiClient";

function getRedirectPath() {
  const requestedPath = new URLSearchParams(window.location.search).get("next");
  return requestedPath?.startsWith("/") &&
    !requestedPath.startsWith("//") &&
    !requestedPath.startsWith("/\\")
    ? requestedPath
    : "/";
}

export default function LoginPage() {
  const router = useRouter();
  const { signIn, status, authError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (status === "authenticated") router.replace(getRedirectPath());
  }, [status, router]);

  async function submit(event) {
    event.preventDefault();
    setFormError("");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setFormError("Enter a valid email address.");
      return;
    }
    if (!password) {
      setFormError("Enter your password.");
      return;
    }

    setSubmitting(true);
    try {
      await signIn(email.trim(), password);
      router.replace(getRedirectPath());
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Sign-in failed. Check your credentials and try again."));
    } finally {
      setSubmitting(false);
    }
  }

  if (status === "loading") {
    return <main className="login-page"><div className="login-card"><span className="spinner" />Restoring session…</div></main>;
  }

  return (
    <main className="login-page">
      <Link href="/" className="login-back">← Designer Costume Studio</Link>
      <section className="login-card">
        <div className="brand-mark login-mark">DS</div>
        <p className="eyebrow">DESIGNER COSTUME STUDIO</p>
        <h1>Welcome back</h1>
        <p className="login-intro">Sign in to your boutique account.</p>
        <form onSubmit={submit} className="login-form" noValidate>
          <label className="form-field">
            <span>Email address</span>
            <input className="form-control" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="username" required />
          </label>
          <label className="form-field">
            <span>Password</span>
            <input className="form-control" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required />
          </label>
          {(formError || authError) && <p className="inline-error" role="alert">{formError || authError}</p>}
          <button className="button button-primary login-submit" type="submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
        <p className="login-security">Your refresh session is held in an HttpOnly cookie. Access tokens stay in memory only.</p>
        <p className="login-register-link">New to the boutique? <Link href="/register">Create a customer account</Link></p>
      </section>
    </main>
  );
}
