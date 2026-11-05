import { Suspense } from "react";
import CostumeListing from "@/components/storefront/CostumeListing";

export default function CostumesPage() {
  return <Suspense fallback={<div className="store-page-container"><div className="store-state">Loading collection…</div></div>}><CostumeListing /></Suspense>;
}
