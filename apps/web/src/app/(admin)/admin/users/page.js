"use client";

import Link from "next/link";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { useAuth } from "@/context/AuthContext";

export default function UsersPage() {
  const { can } = useAuth();
  const canCreate = can("USER_CREATE");

  return (
    <ProtectedRoute permission={canCreate ? "USER_CREATE" : "USER_VIEW"}>
      <PageHeader
        eyebrow="ACCESS CONTROL"
        title="Users"
        description="User administration is limited to the operations currently exposed by the backend."
        action={canCreate && <Link className="button button-primary" href="/admin/users/create">Create user</Link>}
      />
      <section className="content-card capability-card">
        <span className="capability-mark">API GAP</span>
        <h2>User directory is not available</h2>
        <p>The backend currently provides administrative user creation, but no user list, detail, update, or activate/deactivate endpoints. This page does not display placeholder users.</p>
        {!canCreate && <p className="inline-error">Your account does not have USER_CREATE permission.</p>}
      </section>
    </ProtectedRoute>
  );
}
