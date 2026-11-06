"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import {
  deleteAdminCostumeImage,
  getAdminCostumeImageContent,
  getAdminCostumeImages,
  getImageAssetUrl,
  reorderAdminCostumeImages,
  setAdminPrimaryCostumeImage,
  uploadAdminCostumeImage,
} from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function CostumeImageGallery({ costumeId, canUpdate }) {
  const [images, setImages] = useState([]);
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [imageSources, setImageSources] = useState({});

  async function refresh() {
    setImages(await getAdminCostumeImages(costumeId));
  }

  useEffect(() => {
    let active = true;
    getAdminCostumeImages(costumeId)
      .then((result) => { if (active) setImages(result); })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, "Could not load costume images.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [costumeId]);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const objectUrls = [];
    Promise.all(images.map(async (image) => {
      if (!image.fileSize || !image.mimeType) {
        return [image.id, getImageAssetUrl(image.imageUrl)];
      }
      const blob = await getAdminCostumeImageContent(costumeId, image.id, controller.signal);
      if (!active) return null;
      const url = URL.createObjectURL(blob);
      objectUrls.push(url);
      return [image.id, url];
    }))
      .then((entries) => {
        if (active) setImageSources(Object.fromEntries(entries.filter(Boolean)));
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, "Could not load costume image previews."));
      });
    return () => {
      active = false;
      controller.abort();
      objectUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [costumeId, images]);

  async function run(action) {
    setSaving(true);
    setError("");
    try {
      await action();
      await refresh();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "The image change could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  async function upload(event) {
    event.preventDefault();
    if (!file) return;
    const formElement = event.currentTarget;
    await run(async () => {
      await uploadAdminCostumeImage(costumeId, file);
      setFile(null);
      formElement.reset();
    });
  }

  async function move(imageIndex, direction) {
    const next = [...images];
    const target = imageIndex + direction;
    if (target < 0 || target >= next.length) return;
    [next[imageIndex], next[target]] = [next[target], next[imageIndex]];
    await run(() => reorderAdminCostumeImages(costumeId, next.map((image) => image.id)));
  }

  return (
    <section className="content-card">
      <div className="section-heading">
        <div><p className="eyebrow">COSTUME PHOTOGRAPHY</p><h2>Image gallery</h2></div>
        <span className="table-count">{images.length} images</span>
      </div>
      {error && <p className="store-form-message store-form-message-error" role="alert">{error}</p>}
      {canUpdate && (
        <form className="admin-form" onSubmit={upload}>
          <label className="form-field"><span>Add an image · JPEG, PNG or WebP, max 10 MB</span>
            <input className="form-control" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setFile(event.target.files?.[0] || null)} />
          </label>
          <button className="button button-primary" type="submit" disabled={!file || saving}>Upload image</button>
        </form>
      )}
      {loading ? <p className="muted">Loading gallery…</p> : images.length === 0 ? (
        <p className="muted">No gallery images have been uploaded.</p>
      ) : (
        <div className="admin-image-gallery">
          {images.map((image, index) => (
            <article className="admin-image-card" key={image.id}>
              {imageSources[image.id] && <Image src={imageSources[image.id]} alt={image.originalFileName || "Costume"} width={600} height={750} unoptimized />}
              <div><strong>{image.originalFileName || "Costume image"}</strong>{image.primary && <span className="status-pill status-active">Primary</span>}</div>
              {canUpdate && <div className="header-actions">
                {!image.primary && <button className="text-button" type="button" disabled={saving} onClick={() => run(() => setAdminPrimaryCostumeImage(costumeId, image.id))}>Set primary</button>}
                <button className="text-button" type="button" disabled={saving || index === 0} onClick={() => move(index, -1)} aria-label="Move image up">↑</button>
                <button className="text-button" type="button" disabled={saving || index === images.length - 1} onClick={() => move(index, 1)} aria-label="Move image down">↓</button>
                <button className="text-button" type="button" disabled={saving} onClick={() => {
                  if (window.confirm("Delete this costume image?")) run(() => deleteAdminCostumeImage(costumeId, image.id));
                }}>Delete</button>
              </div>}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
