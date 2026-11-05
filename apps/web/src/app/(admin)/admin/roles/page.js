"use client";

import { useEffect, useState } from "react";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getPermissions, getRoles, saveRole } from "@/services/api/roleApi";

const EMPTY_ROLE = { id: "", name: "", description: "", permissions: [] };

export default function RolesPage() {
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [selected, setSelected] = useState(EMPTY_ROLE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function load(signal) {
    const [roleData, permissionData] = await Promise.all([getRoles(signal), getPermissions(signal)]);
    setRoles(roleData);
    setPermissions(permissionData);
  }

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Access-control data could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  function selectRole(role) {
    setSelected({ ...role, permissions: [...role.permissions] });
    setError("");
    setSuccess("");
  }

  function togglePermission(name) {
    setSelected((current) => ({
      ...current,
      permissions: current.permissions.includes(name)
        ? current.permissions.filter((permission) => permission !== name)
        : [...current.permissions, name],
    }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const saved = await saveRole(selected.id || null, {
        name: selected.name.trim(),
        description: selected.description.trim() || null,
        permissions: selected.permissions,
      });
      await load();
      setSelected({ ...saved, permissions: [...saved.permissions] });
      setSuccess("Role saved. New permissions are not automatically granted to existing users.");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Role could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  const columns = [
    { key: "name", label: "Role" },
    { key: "description", label: "Description", render: (role) => role.description || "—" },
    { key: "permissions", label: "Permissions", render: (role) => role.permissions.length },
    { key: "actions", label: "Actions", render: (role) => <button className="text-button" type="button" onClick={() => selectRole(role)}>Edit</button> },
  ];

  const permissionsByModule = permissions.reduce((modules, permission) => {
    modules[permission.module] ||= [];
    modules[permission.module].push(permission);
    return modules;
  }, {});

  return (
    <ProtectedRoute requiredRole="SUPER_ADMIN">
      <PageHeader
        eyebrow="ACCESS CONTROL"
        title="Roles & permissions"
        description="Manage role permissions. Permission definitions are read-only and controlled by the backend."
        action={<button className="button button-primary" type="button" onClick={() => { setSelected(EMPTY_ROLE); setError(""); setSuccess(""); }}>Create role</button>}
      />
      {loading ? <div className="inline-state"><span className="spinner" />Loading access control…</div> : (
        <>
          <section className="content-card">
            <DataTable columns={columns} rows={roles} emptyTitle="No roles found" emptyDescription="No roles are configured." />
          </section>
          <section className="content-card form-card role-editor">
            <h2>{selected.id ? `Edit ${selected.name}` : "Create role"}</h2>
            <form className="admin-form" onSubmit={submit}>
              <div className="form-grid">
                <label className="form-field"><span>Name *</span><input className="form-control" value={selected.name} onChange={(event) => setSelected((current) => ({ ...current, name: event.target.value }))} maxLength={80} required /></label>
                <label className="form-field"><span>Description</span><input className="form-control" value={selected.description || ""} onChange={(event) => setSelected((current) => ({ ...current, description: event.target.value }))} maxLength={500} /></label>
              </div>
              <h3>Permissions</h3>
              <div className="permission-groups">
                {Object.entries(permissionsByModule).map(([module, modulePermissions]) => (
                  <fieldset className="permission-group" key={module}>
                    <legend>{module}</legend>
                    {modulePermissions.map((permission) => (
                      <label key={permission.name} className="permission-option">
                        <input type="checkbox" checked={selected.permissions.includes(permission.name)} onChange={() => togglePermission(permission.name)} />
                        <span><strong>{permission.name}</strong><small>{permission.description || permission.action}</small></span>
                      </label>
                    ))}
                  </fieldset>
                ))}
              </div>
              {error && <p className="inline-error" role="alert">{error}</p>}
              {success && <p className="success-message" role="status">{success}</p>}
              <div className="form-actions"><button className="button button-primary" type="submit" disabled={saving}>{saving ? "Saving…" : "Save role"}</button></div>
            </form>
          </section>
        </>
      )}
    </ProtectedRoute>
  );
}
