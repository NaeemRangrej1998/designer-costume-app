import Link from "next/link";
import ProtectedRoute from "@/components/common/ProtectedRoute";
import StorePageHeading from "@/components/storefront/StorePageHeading";

export default function FavoritesPage() {
  return (
    <ProtectedRoute adminOnly={false} requiredRole="CUSTOMER">
      <div className="store-page-container">
        <StorePageHeading eyebrow="YOUR BOUTIQUE EDIT" title="Saved pieces" description="Keep the looks you love close by." />
        <section className="store-unavailable-card">
          <span className="store-unavailable-icon">♡</span>
          <span className="store-eyebrow">NOT AVAILABLE YET</span>
          <h2>Saved favorites need a backend API</h2>
          <p>Favorites cannot be saved or retrieved because the backend does not currently provide favorites endpoints. Nothing is being stored in this browser.</p>
          <Link className="store-button store-button-dark" href="/costumes">Explore the collection <span>↗</span></Link>
        </section>
      </div>
    </ProtectedRoute>
  );
}
