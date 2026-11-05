"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import PhotoUploader from "./PhotoUploader";
import StorePageHeading from "@/components/storefront/StorePageHeading";
import { StoreError, StoreLoading } from "@/components/storefront/StoreStates";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getCostume, getCostumeVariants } from "@/services/api/costumeApi";

export default function TryOnWorkspace() {
  const searchParams = useSearchParams();
  const costumeId = searchParams.get("costumeId");
  const variantId = searchParams.get("variantId");
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [photo, setPhoto] = useState(null);
  const [loading, setLoading] = useState(Boolean(costumeId));
  const [error, setError] = useState("");
  const [continued, setContinued] = useState(false);

  useEffect(() => {
    if (!costumeId) {
      setCostume(null);
      setVariants([]);
      setLoading(false);
      return undefined;
    }
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      getCostume(costumeId, controller.signal),
      getCostumeVariants(costumeId, controller.signal),
    ])
      .then(([costumeData, availableVariants]) => {
        setCostume(costumeData);
        setVariants(availableVariants);
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(getApiErrorMessage(requestError, "The selected costume could not be loaded."));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [costumeId]);

  const selectedVariant = useMemo(
    () => variants.find((variant) => variant.id === variantId && variant.available),
    [variants, variantId],
  );

  return (
    <div className="store-page-container tryon-page">
      <StorePageHeading eyebrow="A NEW WAY TO EXPLORE" title="Virtual try-on preparation" description="Prepare a photo and choose a look. AI preview processing is not available yet." />
      {loading ? <StoreLoading label="Loading your selected look…" /> :
        error ? <StoreError message={error} onRetry={() => window.location.reload()} /> : null}
      {!loading && !error && (
        <div className="tryon-layout">
          <section className="tryon-main">
            {costume ? (
              <div className="tryon-selected-look">
                <span className="store-eyebrow">YOUR SELECTED LOOK</span>
                <div className="tryon-look-row">
                  <div
                    className={`tryon-costume-thumbnail ${costume.primaryImageUrl ? "tryon-costume-thumbnail-real" : ""}`}
                    style={costume.primaryImageUrl ? { backgroundImage: `url("${costume.primaryImageUrl}")` } : undefined}
                    role={costume.primaryImageUrl ? "img" : undefined}
                    aria-label={costume.primaryImageUrl ? costume.name : undefined}
                  >
                    {!costume.primaryImageUrl && "DS"}
                  </div>
                  <div>
                    <strong>{costume.name}</strong>
                    <span>{costume.categoryName || "Designer occasionwear"}</span>
                    <span>{selectedVariant ? `${selectedVariant.colorName} · ${selectedVariant.sizeName}` : "No available variant selected"}</span>
                  </div>
                  <Link href={`/costumes/${costume.id}`} className="tryon-edit-link">Change</Link>
                </div>
              </div>
            ) : (
              <div className="tryon-select-costume">
                <span className="store-step-number">01</span>
                <div><h2>Choose a costume</h2><p>Select a costume and available variant before preparing your photo.</p></div>
                <Link className="store-button store-button-outline" href="/costumes">Browse costumes <span>→</span></Link>
              </div>
            )}

            <div className="tryon-photo-section">
              <div className="tryon-step-heading"><span className="store-step-number">02</span><div><h2>Add a photo</h2><p>Choose a clear photo to prepare for a future virtual try-on.</p></div></div>
              <PhotoUploader file={photo} onChange={(next) => { setPhoto(next); setContinued(false); }} />
              <div className="tryon-photo-guidance">
                <strong>For the best preparation</strong>
                <ul>
                  <li>Use a well-lit, full-length photo with a simple background.</li>
                  <li>Stand naturally and keep your outfit visible.</li>
                  <li>Only use a photo you have permission to use.</li>
                </ul>
              </div>
            </div>

            <div className="tryon-privacy-note">
              <span aria-hidden="true">⌑</span>
              <p><strong>Your privacy matters.</strong> This preview is local to your browser session. The photo is not uploaded or saved. Remove it at any time.</p>
            </div>

            <button
              className="store-button store-button-dark tryon-continue"
              disabled={!photo || !costume || !selectedVariant}
              onClick={() => setContinued(true)}
            >
              Continue to preview <span>→</span>
            </button>
            {continued && (
              <div className="tryon-coming-soon" role="status">
                <span className="store-eyebrow">PREPARATION COMPLETE</span>
                <h2>Virtual try-on processing will be available soon.</h2>
                <p>No image was uploaded, and no AI result was generated.</p>
                <button className="photo-remove-button" onClick={() => { setPhoto(null); setContinued(false); }}>Remove my photo</button>
              </div>
            )}
          </section>
          <aside className="tryon-side-note">
            <span className="store-eyebrow">HOW IT WILL WORK</span>
            <div><b>01</b><span>Choose an available costume variant.</span></div>
            <div><b>02</b><span>Prepare a photo that meets the guidance.</span></div>
            <div><b>03</b><span>AI preview processing will be added in a future phase.</span></div>
            <p>For now, the photo stays in your browser and this page is a preparation experience only.</p>
          </aside>
        </div>
      )}
    </div>
  );
}
