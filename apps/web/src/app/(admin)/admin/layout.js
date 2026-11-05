import AdminShell from "@/components/layout/AdminShell";
import ProtectedRoute from "@/components/common/ProtectedRoute";

export default function AdminLayout({ children }) {
  return (
    <ProtectedRoute>
      <AdminShell>{children}</AdminShell>
    </ProtectedRoute>
  );
}
