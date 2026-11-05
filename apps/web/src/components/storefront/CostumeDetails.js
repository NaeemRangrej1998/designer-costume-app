"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import VariantSelector from "./VariantSelector";
import StorePageHeading from "./StorePageHeading";
import { StoreEmpty, StoreError, StoreLoading } from "./StoreStates";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getCostume, getCostumeVariants } from "@/services/api/costumeApi";

function priceText(price) {
  if (price == null || price === "") return null;
  const parsed = Number(price);
  return Number.isFinite(parsed)
    ? new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(parsed)
    : null;
}

export default function CostumeDetails() {
  const { id } = useParams();
  const router = useRouter();
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectionError, setSelectionError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      getCostume(id, controller.signal),
      getCostumeVariants(id, controller.signal),
    ])
      .then(([costumeData, availableVariants]) => {
        setCostume(costumeData);
        setVariants(availableVariants);
        setSelectedVariant(availableVariants[0] || null);
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "This costume could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id]);

  const price = priceText(selectedVariant?.price ?? costume?.basePrice);
  const categoryLabel = useMemo(() => costume?.categoryName || "Designer occasionwear", [costume]);

  function beginTryOn() {
    if (!selectedVariant || !selectedVariant.available) {
      setSelectionError("Choose an available color and size before continuing.");
      return;
    }
    router.push(`/try-on?costumeId=${encodeURIComponent(costume.id)}&variantId=${encodeURIComponent(selectedVariant.id)}`);
  }

  if (loading) return <div className="store-page-container"><StoreLoading label="Opening this piece…" /></div>;
  if (error) return <div className="store-page-container"><StoreError message={error} onRetry={() => window.location.reload()} /></div>;
  if (!costume) return null;

  return (
    <div className="store-page-container">
      <div className="store-breadcrumb"><Link href="/costumes">Collection</Link><span>/</span><span>{costume.name}</span></div>
      <div className="product-layout">
        <div className="product-gallery">
          <div
            className={`product-main-image ${costume.primaryImageUrl ? "product-main-image-real" : ""}`}
            style={costume.primaryImageUrl ? { backgroundImage: `url("${costume.primaryImageUrl}")` } : undefined}
            role={costume.primaryImageUrl ? "img" : undefined}
            aria-label={costume.primaryImageUrl ? costume.name : undefined}
          >
            {!costume.primaryImageUrl && <span>IMAGE<br />COMING SOON</span>}
            <span className="product-image-label">THE BOUTIQUE COLLECTION</span>
          </div>
          <p className="gallery-note">Additional gallery images are not available in the current catalog API.</p>
        </div>
        <section className="product-information">
          <span className="store-eyebrow">{categoryLabel}</span>
          <h1>{costume.name}</h1>
          {costume.sku && <p className="product-sku">STYLE NO. {costume.sku}</p>}
          {price ? <div className="product-price">{price}<small>Currency not specified</small></div> : <div className="product-price product-price-request">Price on request</div>}
          <p className="product-description">{costume.description || "Please contact the boutique for more information about this piece."}</p>
          <div className="product-divider" />

          {variants.length ? (
            <>
              <VariantSelector
                variants={variants}
                selectedVariant={selectedVariant}
                onSelect={(variant) => {
                  setSelectedVariant(variant);
                  setSelectionError("");
                }}
                onRequireSelection={() => setSelectionError("Please contact the boutique for personal sizing guidance.")}
              />
              {selectionError && <p className="store-form-message" role="status">{selectionError}</p>}
            </>
          ) : (
            <StoreEmpty title="No available variants" message="This piece currently has no available color and size combinations." />
          )}

          <button className="store-button store-button-dark product-tryon-button" onClick={beginTryOn} disabled={!variants.length}>
            Try this costume virtually <span>↗</span>
          </button>
          <p className="product-tryon-note">Photo preparation only. AI virtual try-on processing is coming soon.</p>
          <div className="product-secondary-actions">
            <Link href={`/contact?costumeId=${encodeURIComponent(costume.id)}`}>Ask the boutique about this piece <span>→</span></Link>
            <button type="button" disabled title="Favorites are unavailable until a backend favorites API exists">♡ Save to favorites · unavailable</button>
          </div>
          <div className="product-meta">
            <span>Category <strong>{categoryLabel}</strong></span>
            <span>Availability <strong>{variants.some((variant) => variant.available) ? "Selected sizes available" : "Currently unavailable"}</strong></span>
          </div>
        </section>
      </div>
    </div>
  );
}
