"use client";

export default function VariantSelector({ variants, selectedVariant, onSelect, onRequireSelection }) {
  const colors = [...new Map(variants.map((variant) => [variant.colorName, variant])).values()];
  const selectedColor = selectedVariant?.colorName || colors[0]?.colorName || "";
  const sizes = variants.filter((variant) => variant.colorName === selectedColor);

  function chooseColor(colorName) {
    const currentSizeAvailable = sizes.some((variant) =>
      variant.colorName === colorName && variant.sizeName === selectedVariant?.sizeName,
    );
    const next = variants.find((variant) =>
      variant.colorName === colorName &&
      (currentSizeAvailable ? variant.sizeName === selectedVariant?.sizeName : true),
    );
    if (next) onSelect(next);
  }

  return (
    <div className="variant-selector">
      <div className="variant-group">
        <div className="variant-label"><span>Color</span><strong>{selectedColor || "Select a color"}</strong></div>
        <div className="variant-colors">
          {colors.map((variant) => (
            <button
              type="button"
              key={variant.colorName}
              className={`variant-color ${selectedColor === variant.colorName ? "variant-color-selected" : ""}`}
              onClick={() => chooseColor(variant.colorName)}
              aria-label={`Select ${variant.colorName}`}
              aria-pressed={selectedColor === variant.colorName}
            >
              <span style={{ backgroundColor: /^#[0-9a-f]{6}$/i.test(variant.colorHexCode || "") ? variant.colorHexCode : "#ddd8cf" }} />
            </button>
          ))}
        </div>
      </div>
      <div className="variant-group">
        <div className="variant-label"><span>Size</span><button type="button" className="size-help" onClick={onRequireSelection}>Size guide</button></div>
        <div className="variant-sizes">
          {sizes.map((variant) => (
            <button
              type="button"
              key={`${variant.colorName}-${variant.sizeName}`}
              className={`variant-size ${selectedVariant?.id === variant.id ? "variant-size-selected" : ""}`}
              onClick={() => onSelect(variant)}
              aria-pressed={selectedVariant?.id === variant.id}
              disabled={!variant.available}
            >
              {variant.sizeName}
            </button>
          ))}
        </div>
      </div>
      {selectedVariant && (
        <p className="variant-availability">
          <span className="availability-dot" /> Available · {selectedVariant.colorName} / {selectedVariant.sizeName}
        </p>
      )}
    </div>
  );
}
