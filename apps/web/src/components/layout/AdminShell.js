"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { getApiErrorMessage } from "@/services/api/apiClient";

const navigation = [
  { label: "Dashboard", href: "/admin", permission: null },
  { label: "Categories", href: "/admin/categories", permission: "CATEGORY_VIEW" },
  { label: "Costumes", href: "/admin/costumes", permission: "COSTUME_VIEW" },
  { label: "Users", href: "/admin/users", permission: ["USER_VIEW", "USER_CREATE"] },
  { label: "Colors & sizes", href: "/admin/catalog-options", permission: "COSTUME_VIEW" },
  { label: "Inquiries", href: "/admin/inquiries", permission: "INQUIRY_VIEW" },
  { label: "Roles & permissions", href: "/admin/roles", role: "SUPER_ADMIN" },
];

export default function AdminShell({ children }) {
  const pathname = usePathname();
  const { user, can, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  const visibleNavigation = navigation.filter((item) => {
    const { permission } = item;
    if (item?.role && !user?.roles?.includes(item.role)) return false;
    if (!permission) return true;
    return Array.isArray(permission)
      ? permission.some((item) => can(item))
      : can(permission);
  });

  async function handleSignOut() {
    setLogoutError("");
    try {
      await signOut();
    } catch (error) {
      setLogoutError(getApiErrorMessage(error, "Could not contact the logout endpoint."));
    }
  }

  return (
    <div className="admin-frame">
      {menuOpen && <button className="mobile-backdrop" aria-label="Close navigation" onClick={() => setMenuOpen(false)} />}
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <Link className="brand-lockup" href="/admin" onClick={() => setMenuOpen(false)}>
          <span className="brand-mark">DS</span>
          <span><strong>Costume Studio</strong><small>ADMINISTRATION</small></span>
        </Link>
        <div className="sidebar-label">WORKSPACE</div>
        <nav className="sidebar-nav" aria-label="Admin navigation">
          {visibleNavigation.map((item) => {
            const active = item.href === "/admin"
              ? pathname === item.href
              : pathname.startsWith(item.href);
            return (
              <Link
                className={`sidebar-link ${active ? "sidebar-link-active" : ""}`}
                href={item.href}
                key={item.href}
                onClick={() => setMenuOpen(false)}
              >
                <span className="nav-indicator" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="sidebar-footer">
          <div className="sidebar-footer-note">Phase 5 · Admin workspace</div>
          <button className="sidebar-signout" onClick={handleSignOut}>Sign out <span aria-hidden="true">↗</span></button>
          {logoutError && <p className="sidebar-error" role="alert">{logoutError}</p>}
        </div>
      </aside>

      <section className="admin-main">
        <header className="topbar">
          <button className="menu-toggle" onClick={() => setMenuOpen(true)} aria-label="Open navigation">☰</button>
          <div className="topbar-breadcrumb">STUDIO <span>/</span> ADMIN</div>
          <div className="topbar-user">
            <span className="avatar">{(user?.fullName || "U").slice(0, 1).toUpperCase()}</span>
            <span className="topbar-user-name">{user?.fullName || user?.email}</span>
            <span className="role-pill">{user?.roles?.[0]?.replaceAll("_", " ") || "ADMIN"}</span>
          </div>
        </header>
        <main className="admin-content">{children}</main>
      </section>
    </div>
  );
}
