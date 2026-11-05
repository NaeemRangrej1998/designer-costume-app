"use client";

import { useEffect, useState } from "react";
import DataTable from "@/components/common/DataTable";
import PageHeader from "@/components/common/PageHeader";
import Pagination from "@/components/common/Pagination";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getInquiries, updateInquiryStatus } from "@/services/api/inquiryApi";
import { useAuth } from "@/context/AuthContext";

const STATUSES = ["NEW", "CONTACTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export default function InquiriesPage() {
  const { can } = useAuth();
  const [pageData, setPageData] = useState({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getInquiries({ page, size: 10, search: query, status: statusFilter, signal: controller.signal })
      .then(setPageData)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "Could not load inquiries."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, query, statusFilter]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  async function changeStatus(inquiry, status) {
    try {
      const updated = await updateInquiryStatus(inquiry.id, status);
      setPageData((current) => ({
        ...current,
        content: current.content.map((item) => item.id === updated.id ? updated : item),
      }));
      setActionError("");
    } catch (requestError) {
      setActionError(getApiErrorMessage(requestError, "Inquiry status could not be updated."));
    }
  }

  const columns = [
    { key: "customerName", label: "Customer" },
    { key: "email", label: "Email" },
    { key: "message", label: "Message", render: (inquiry) => <span className="message-preview" title={inquiry.message}>{inquiry.message}</span> },
    { key: "costumeName", label: "Costume", render: (inquiry) => inquiry.costumeName || "—" },
    {
      key: "status",
      label: "Status",
      render: (inquiry) => (
        <select
          className="form-control status-select"
          value={inquiry.status}
          aria-label={`Status for inquiry from ${inquiry.customerName}`}
          disabled={!can("INQUIRY_UPDATE")}
          onChange={(event) => changeStatus(inquiry, event.target.value)}
        >
          {STATUSES.map((status) => <option value={status} key={status}>{status.replaceAll("_", " ")}</option>)}
        </select>
      ),
    },
    { key: "createdAt", label: "Received", render: (inquiry) => new Date(inquiry.createdAt).toLocaleDateString() },
  ];

  return (
    <ProtectedRoute permission="INQUIRY_VIEW">
      <PageHeader eyebrow="CUSTOMER CARE" title="Inquiries" description="Review customer questions and update their status." />
      <section className="content-card">
        <div className="table-toolbar">
          <form className="search-form" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="inquiry-search">Search inquiries</label>
            <input id="inquiry-search" className="form-control search-control" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search customer or email" maxLength={100} />
            <button className="button button-primary" type="submit">Search</button>
          </form>
          <label className="filter-control"><span>Status</span><select className="form-control" value={statusFilter} onChange={(event) => { setPage(0); setStatusFilter(event.target.value); }}><option value="">All statuses</option>{STATUSES.map((status) => <option value={status} key={status}>{status.replaceAll("_", " ")}</option>)}</select></label>
          <span className="table-count">{pageData.totalElements} inquiries</span>
        </div>
        {actionError && <p className="inline-error" role="alert">{actionError}</p>}
        <DataTable columns={columns} rows={pageData.content} loading={loading} error={error} emptyTitle="No inquiries found" emptyDescription="Customer submissions will appear here when the inquiry submission API is available." />
        <Pagination page={pageData.page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onChange={setPage} />
      </section>
    </ProtectedRoute>
  );
}
