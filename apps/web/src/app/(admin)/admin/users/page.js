"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { useAuth } from "@/context/AuthContext";
import { deactivateAdminUser, getAdminUsers } from "@/services/api/userApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function UsersPage() {
  const { can } = useAuth();
  const canCreate = can("USER_CREATE");
  const canView = can("USER_VIEW");
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [activeFilter, setActiveFilter] = useState("true");
  const [loading, setLoading] = useState(canView);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    if (!canView) return undefined;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getAdminUsers({
      page,
      size: 10,
      search: query,
      active: activeFilter === "" ? undefined : activeFilter === "true",
      signal: controller.signal,
    })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load users."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [canView, page, query, activeFilter]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  async function handleDeactivate(user) {
    if (!window.confirm(`Deactivate ${user.fullName} (${user.email})?`)) return;
    setActionError("");
    try {
      await deactivateAdminUser(user.id);
      setPageData((current) => ({
        ...current,
        content: current.content.filter((item) => item.id !== user.id),
        totalElements: Math.max(0, current.totalElements - 1),
      }));
    } catch (requestError) {
      setActionError(getApiErrorMessage(requestError, "User could not be deactivated."));
    }
  }

  const columns = [
    { key: "fullName", label: "Name" },
    { key: "email", label: "Email" },
    { key: "phoneNumber", label: "Phone", render: (user) => user.phoneNumber || "—" },
    { key: "roles", label: "Roles", render: (user) => [...(user.roles || [])].join(", ") || "—" },
    { key: "active", label: "Status", render: (user) => <span className={`status-pill ${user.active ? "status-active" : "status-inactive"}`}>{user.active ? "Active" : "Inactive"}</span> },
    {
      key: "actions",
      label: "Actions",
      render: (user) => (
        <div className="table-actions">
          {can("USER_UPDATE") && <Link className="text-link" href={`/admin/users/${user.id}/edit`}>Edit</Link>}
          {user.active && can("USER_DELETE") && (
            <button className="text-button text-danger" type="button" onClick={() => handleDeactivate(user)}>Deactivate</button>
          )}
        </div>
      ),
    },
  ];

  return (
    <ProtectedRoute permission={canView ? "USER_VIEW" : "USER_CREATE"}>
      <PageHeader
        eyebrow="ACCESS CONTROL"
        title="Users"
        description="Manage user accounts and their assigned roles."
        action={canCreate && <Link className="button button-primary" href="/admin/users/create">Create user</Link>}
      />
      {canView ? (
        <section className="content-card">
          <div className="table-toolbar">
            <form className="search-form" onSubmit={submitSearch}>
              <label className="sr-only" htmlFor="user-search">Search users</label>
              <input id="user-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name or email" maxLength={100} />
              <button className="button button-primary" type="submit">Search</button>
            </form>
            <label className="filter-control"><span>Status</span><select className="form-control" value={activeFilter} onChange={(event) => { setPage(0); setActiveFilter(event.target.value); }}><option value="true">Active</option><option value="false">Inactive</option><option value="">All</option></select></label>
            <span className="table-count">{pageData.totalElements} users</span>
          </div>
          {actionError && <p className="inline-error" role="alert">{actionError}</p>}
          <DataTable columns={columns} rows={pageData.content} loading={loading} error={error} emptyTitle="No users found" emptyDescription="Try a different search or status filter." />
          <Pagination page={pageData.page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onChange={setPage} />
        </section>
      ) : (
        <section className="content-card capability-card">
          <span className="capability-mark">LIMITED ACCESS</span>
          <h2>User directory requires USER_VIEW</h2>
          <p>Your account can create users but cannot view the user directory.</p>
        </section>
      )}
    </ProtectedRoute>
  );
}
