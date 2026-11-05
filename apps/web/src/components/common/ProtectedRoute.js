"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export default function ProtectedRoute({ permission, requiredRole, adminOnly = true, children }) {
  const { status, user, isAdministrator, can, authError } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") {
      const currentPath = typeof window === "undefined"
        ? pathname
        : `${window.location.pathname}${window.location.search}`;
      router.replace(`/login?next=${encodeURIComponent(currentPath)}`);
    }
  }, [status, pathname, router]);

  if (status === "loading") {
    return <main className="page-state"><span className="spinner" />Checking your session…</main>;
  }
  if (status === "error") {
    return (
      <main className="page-state">
        <section className="state-card">
          <h1>Authentication service unavailable</h1>
          <p>{authError}</p>
          <button className="button button-secondary" onClick={() => window.location.reload()}>
            Try again
          </button>
        </section>
      </main>
    );
  }
  if (!user || status !== "authenticated") return null;
  const hasRequiredRole = !requiredRole || user.roles?.includes(requiredRole);
  if ((adminOnly && !isAdministrator) || !hasRequiredRole || (permission && !can(permission))) {
    return (
      <main className="page-state">
        <section className="state-card">
          <span className="eyebrow">403 · Access restricted</span>
          <h1>You don’t have permission to view this page.</h1>
          <p>Access is checked by the backend as well as this interface.</p>
          <a className="button button-secondary" href={adminOnly ? "/admin" : "/"}>{adminOnly ? "Return to dashboard" : "Return to the boutique"}</a>
        </section>
      </main>
    );
  }
  return children;
}
