"use client";

import { useEffect, useState } from "react";
import Image from "next/image";

const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_FILE_SIZE = 10 * 1024 * 1024;

export default function PhotoUploader({ file, onChange }) {
  const [error, setError] = useState("");
  const [previewUrl, setPreviewUrl] = useState("");

  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      return undefined;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  function selectFile(event) {
    const next = event.target.files?.[0] || null;
    event.target.value = "";
    setError("");
    if (!next) return;
    if (!ACCEPTED_TYPES.includes(next.type)) {
      setError("Choose a JPEG, PNG, or WebP image.");
      return;
    }
    if (next.size > MAX_FILE_SIZE) {
      setError("Choose an image smaller than 10 MB.");
      return;
    }
    onChange(next);
  }

  return (
    <div className="photo-uploader">
      {previewUrl ? (
        <div className="photo-preview-wrap">
          <Image className="photo-preview" src={previewUrl} alt="Selected photo preview" width={720} height={900} unoptimized />
          <div className="photo-preview-controls">
            <span>{file.name} · {(file.size / (1024 * 1024)).toFixed(1)} MB</span>
            <div>
              <label className="photo-change-button">Change photo<input type="file" accept="image/jpeg,image/png,image/webp" onChange={selectFile} /></label>
              <button type="button" className="photo-remove-button" onClick={() => { onChange(null); setError(""); }}>Remove</button>
            </div>
          </div>
        </div>
      ) : (
        <label className="photo-dropzone">
          <span className="photo-upload-icon">↑</span>
          <strong>Choose a clear, full-length photo</strong>
          <span>JPEG, PNG or WebP · maximum 10 MB</span>
          <span className="photo-browse-button">Choose photo</span>
          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={selectFile} />
        </label>
      )}
      {error && <p className="store-form-message store-form-message-error" role="alert">{error}</p>}
    </div>
  );
}
