"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import PhotoUploader from "./PhotoUploader";
import StorePageHeading from "@/components/storefront/StorePageHeading";
import { StoreError, StoreLoading } from "@/components/storefront/StoreStates";
import { getApiErrorMessage } from "@/services/api/apiClient";
import { getCostume, getCostumeVariants, getImageAssetUrl } from "@/services/api/costumeApi";
import { deleteCustomerPhoto, getCustomerPhotoContent, getCustomerPhotos, uploadCustomerPhoto } from "@/services/api/customerPhotosApi";
import { useAuth } from "@/context/AuthContext";

export default function TryOnWorkspace() {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const costumeId = searchParams.get("costumeId");
  const variantId = searchParams.get("variantId");
  const [costume, setCostume] = useState(null);
  const [variants, setVariants] = useState([]);
  const [photo, setPhoto] = useState(null);
  const [selectedSavedPhotoId, setSelectedSavedPhotoId] = useState(null);
  const [loading, setLoading] = useState(Boolean(costumeId));
  const [error, setError] = useState("");
  const [continued, setContinued] = useState(false);
  const [savedPhotos, setSavedPhotos] = useState([]);
  const [photoAction, setPhotoAction] = useState("");
  const [photoMessage, setPhotoMessage] = useState("");
  const isCustomer = user?.roles?.includes("CUSTOMER");

  useEffect(() => {
    if (!isCustomer) {
      setSavedPhotos([]);
      return undefined;
    }
    const controller = new AbortController();
    getCustomerPhotos(controller.signal)
      .then(setSavedPhotos)
      .catch((requestError) => {
        if (!controller.signal.aborted) setPhotoMessage(getApiErrorMessage(requestError, "Your private photo library could not be loaded."));
      });
    return () => controller.abort();
  }, [isCustomer]);

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

  async function savePhotoToLibrary() {
    if (!photo || !isCustomer) return;
    setPhotoAction("upload");
    setPhotoMessage("");
    try {
      const savedPhoto = await uploadCustomerPhoto(photo);
      setSelectedSavedPhotoId(savedPhoto.id);
      setSavedPhotos(await getCustomerPhotos());
      setPhotoMessage("Photo saved to your private library on this service.");
    } catch (requestError) {
      setPhotoMessage(getApiErrorMessage(requestError, "The photo could not be saved."));
    } finally {
      setPhotoAction("");
    }
  }

  async function selectSavedPhoto(savedPhoto) {
    setPhotoAction(savedPhoto.id);
    setPhotoMessage("");
    try {
      const blob = await getCustomerPhotoContent(savedPhoto.id);
      const savedFile = new File([blob], savedPhoto.originalFileName, { type: savedPhoto.mimeType });
      setPhoto(savedFile);
      setSelectedSavedPhotoId(savedPhoto.id);
      setContinued(false);
    } catch (requestError) {
      setPhotoMessage(getApiErrorMessage(requestError, "The saved photo could not be opened."));
    } finally {
      setPhotoAction("");
    }
  }

  async function removeSavedPhoto(photoId) {
    setPhotoAction(photoId);
    setPhotoMessage("");
    try {
      await deleteCustomerPhoto(photoId);
      setSavedPhotos((current) => current.filter((savedPhoto) => savedPhoto.id !== photoId));
      if (selectedSavedPhotoId === photoId) {
        setPhoto(null);
        setSelectedSavedPhotoId(null);
      }
      setPhotoMessage("Photo deleted from your private library.");
    } catch (requestError) {
      setPhotoMessage(getApiErrorMessage(requestError, "The saved photo could not be deleted."));
    } finally {
      setPhotoAction("");
    }
  }

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
                    style={costume.primaryImageUrl ? { backgroundImage: `url("${getImageAssetUrl(costume.primaryImageUrl)}")` } : undefined}
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
              <PhotoUploader file={photo} onChange={(next) => { setPhoto(next); setSelectedSavedPhotoId(null); setContinued(false); }} />
              {photo && isCustomer && !selectedSavedPhotoId && <button className="store-button store-button-outline" type="button" disabled={Boolean(photoAction)} onClick={savePhotoToLibrary}>
                {photoAction === "upload" ? "Saving photo…" : "Save to my private photo library"}
              </button>}
              {isCustomer && savedPhotos.length > 0 && <div className="saved-photo-library">
                <h3>Your private photos</h3>
                <ul>{savedPhotos.map((savedPhoto) => <li key={savedPhoto.id}>
                  <span>{savedPhoto.originalFileName} · {(savedPhoto.fileSize / (1024 * 1024)).toFixed(1)} MB</span>
                  <button type="button" disabled={Boolean(photoAction)} onClick={() => selectSavedPhoto(savedPhoto)}>Use photo</button>
                  <button type="button" disabled={Boolean(photoAction)} onClick={() => removeSavedPhoto(savedPhoto.id)}>Delete</button>
                </li>)}</ul>
              </div>}
              {photoMessage && <p className="store-form-message" role="status">{photoMessage}</p>}
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
              <p><strong>Your privacy matters.</strong> Photos stay in this browser unless you explicitly save one to your private, account-only library. Saved photos are not sent to the AI service. Remove local selections at any time.</p>
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
