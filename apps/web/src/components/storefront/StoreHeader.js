"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/context/AuthContext";

const links = [
  { href: "/", label: "Home" },
  { href: "/categories", label: "Categories" },
  { href: "/costumes", label: "Collections" },
  { href: "/try-on", label: "Virtual Try-On" },
  { href: "/contact", label: "Contact" },
];

export default function StoreHeader() {
  const { user, status, signOut } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="store-header">
      <div className="store-header-inner">
        <Link className="store-brand" href="/" onClick={() => setMenuOpen(false)}>
          <span className="store-brand-mark">DS</span>
          <span><strong>Designer Costume</strong><small>STUDIO</small></span>
        </Link>
        <button
          className="store-menu-toggle"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-label={menuOpen ? "Close navigation" : "Open navigation"}
        >
          <span />
          <span />
        </button>
        <nav className={`store-navigation ${menuOpen ? "store-navigation-open" : ""}`} aria-label="Main navigation">
          {links.map((link) => (
            <Link href={link.href} key={link.href} onClick={() => setMenuOpen(false)}>
              {link.label}
            </Link>
          ))}
          {user?.roles?.includes("CUSTOMER") && (
            <Link href="/favorites" onClick={() => setMenuOpen(false)}>Favorites</Link>
          )}
        </nav>
        <div className="store-header-actions">
          <Link className="store-search-link" href="/costumes" aria-label="Search costumes">
            <span aria-hidden="true">⌕</span>
          </Link>
          {status === "authenticated" && user ? (
            <>
              <span className="store-account-name">{user.fullName?.split(" ")[0] || "Account"}</span>
              <button className="store-account-link" onClick={() => signOut().catch(() => {})}>Sign out</button>
            </>
          ) : (
            <Link className="store-account-link" href="/login">Sign in</Link>
          )}
        </div>
      </div>
    </header>
  );
}
