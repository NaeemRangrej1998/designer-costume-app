"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import PageHeader from "@/components/common/PageHeader";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getDashboardSummary, getRecentAdminActivity } from "@/services/api/dashboardApi";
import { useAuth } from "@/context/AuthContext";

const numberFormat = new Intl.NumberFormat();
const dateTimeFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
const metrics = [
  { key: "activeCostumes", label: "Active costumes", permission: "COSTUME_VIEW" },
  { key: "activeCategories", label: "Active categories", permission: "CATEGORY_VIEW" },
  { key: "availableVariants", label: "Available variants", permission: "COSTUME_VIEW" },
  { key: "activeCustomers", label: "Active customers", permission: "USER_VIEW" },
  { key: "totalInquiries", label: "Total inquiries", permission: "INQUIRY_VIEW" },
  { key: "newInquiries", label: "New inquiries", permission: "INQUIRY_VIEW" },
];

export default function DashboardPage() {
  const { can } = useAuth();
  const canViewUsers = can("USER_VIEW");
  const [summary, setSummary] = useState(null);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activityLoading, setActivityLoading] = useState(true);
  const [error, setError] = useState("");
  const [activityError, setActivityError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getDashboardSummary(controller.signal)
      .then(setSummary)
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setError(getApiErrorMessage(requestError, "Could not load dashboard metrics."));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!canViewUsers) {
      setActivityLoading(false);
      return undefined;
    }

    const controller = new AbortController();
    getRecentAdminActivity(controller.signal)
      .then(setActivity)
      .catch((requestError) => {
        if (!controller.signal.aborted) {
          setActivityError(getApiErrorMessage(requestError, "Could not load recent activity."));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setActivityLoading(false);
      });
    return () => controller.abort();
  }, [canViewUsers]);

  function retry() {
    setLoading(true);
    setError("");
    getDashboardSummary()
      .then(setSummary)
      .catch((requestError) => setError(getApiErrorMessage(requestError, "Could not load dashboard metrics.")))
      .finally(() => setLoading(false));
  }

  const distribution = summary?.costumeDistribution || [];
  const largestCategory = Math.max(1, ...distribution.map((item) => item.costumeCount));

  return (
    <>
      <PageHeader
        eyebrow="OVERVIEW"
        title="Dashboard"
        description="A clear view of your boutique workspace."
      />

      <section className="metric-grid" aria-label="Dashboard statistics">
        {metrics.map(({ key, label }) => (
          <article className={`metric-card${summary?.[key] == null ? " metric-unavailable" : ""}`} key={key}>
            <span className="metric-label">{label}</span>
            <strong>{loading ? "—" : summary?.[key] == null ? "Restricted" : numberFormat.format(summary[key])}</strong>
            <span className="metric-note">
              {loading ? "Loading live data…" : summary?.[key] == null ? "Permission required" : "Live database total"}
            </span>
          </article>
        ))}
      </section>

      {error && (
        <section className="content-card dashboard-error" role="alert">
          <p>{error}</p>
          <button type="button" className="button button-secondary" onClick={retry}>Try again</button>
        </section>
      )}

      <div className="dashboard-grid">
        <section className="content-card dashboard-panel">
          <div className="section-heading">
            <div><p className="eyebrow">CATALOG</p><h2>Costumes by category</h2></div>
            <span className="status-pill status-active">{loading ? "Loading" : "Live data"}</span>
          </div>
          {distribution.length ? (
            <ul className="dashboard-distribution">
              {distribution.map((item) => (
                <li key={item.categoryName}>
                  <div className="dashboard-distribution-label">
                    <span>{item.categoryName}</span>
                    <strong>{numberFormat.format(item.costumeCount)}</strong>
                  </div>
                  <div className="dashboard-distribution-track" aria-hidden="true">
                    <span style={{ width: `${(item.costumeCount / largestCategory) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className="api-placeholder">
              <span className="placeholder-line" />
              <p>{loading ? "Loading category totals…" : "No category distribution is available to this account."}</p>
            </div>
          )}
          <div className="dashboard-panel-link">
            <Link href="/admin/costumes" className="text-link">View active costumes <span>→</span></Link>
          </div>
        </section>

        <section className="content-card dashboard-panel">
          <div className="section-heading">
            <div><p className="eyebrow">CUSTOMER CARE</p><h2>Inquiry status</h2></div>
            <span className={`status-pill ${summary?.totalInquiries == null ? "status-muted" : "status-active"}`}>
              {summary?.totalInquiries == null ? "Restricted" : "Live data"}
            </span>
          </div>
          <div className="api-placeholder">
            <p>
              {summary?.totalInquiries == null
                ? "Inquiry totals require INQUIRY_VIEW permission."
                : `${numberFormat.format(summary.newInquiries)} new of ${numberFormat.format(summary.totalInquiries)} inquiries.`}
            </p>
            {can("INQUIRY_VIEW")
              ? <Link href="/admin/inquiries" className="text-link">Manage inquiries <span>→</span></Link>
              : <span className="text-link text-link-disabled">INQUIRY_VIEW permission required</span>}
          </div>
        </section>
      </div>

      {canViewUsers && (
        <section className="content-card dashboard-panel activity-panel">
          <div className="section-heading">
            <div><p className="eyebrow">ADMINISTRATION</p><h2>Recent activity</h2></div>
            <span className={`status-pill ${activityError ? "status-inactive" : "status-active"}`}>
              {activityError ? "Unavailable" : "Live data"}
            </span>
          </div>
          {activityError ? (
            <p className="activity-message" role="alert">{activityError}</p>
          ) : activityLoading ? (
            <p className="activity-message">Loading recent activity…</p>
          ) : activity.length ? (
            <ul className="activity-list">
              {activity.map((event) => (
                <li key={event.id}>
                  <div>
                    <strong>{event.action.replaceAll("_", " ")} {event.resourceType.toLowerCase().replaceAll("_", " ")}</strong>
                    <span>{event.actorName}</span>
                  </div>
                  <time dateTime={event.createdAt}>{dateTimeFormat.format(new Date(event.createdAt))}</time>
                </li>
              ))}
            </ul>
          ) : (
            <p className="activity-message">No administrative changes have been recorded yet.</p>
          )}
        </section>
      )}

      <section className="content-card integration-note">
        <span className="integration-icon" aria-hidden="true">i</span>
        <div>
          <h2>Dashboard connected to live data</h2>
          <p>Metrics are permission-filtered and calculated from the database. Administrative changes are recorded for this activity feed; configurable application settings are not implemented.</p>
        </div>
      </section>
    </>
  );
}
