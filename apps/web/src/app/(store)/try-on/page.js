import { Suspense } from "react";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import TryOnWorkspace from "@/components/try-on/TryOnWorkspace";

export default function TryOnPage() {
  return (
    <ProtectedRoute adminOnly={false} requiredRole="CUSTOMER">
      <Suspense fallback={<div className="store-page-container"><div className="store-state">Preparing try-on workspace…</div></div>}>
        <TryOnWorkspace />
      </Suspense>
    </ProtectedRoute>
  );
}
