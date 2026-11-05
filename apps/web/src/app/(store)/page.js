"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import CategoryCard from "@/components/storefront/CategoryCard";
import CostumeGrid from "@/components/storefront/CostumeGrid";
import StorePageHeading from "@/components/storefront/StorePageHeading";
import { StoreEmpty, StoreError, StoreLoading } from "@/components/storefront/StoreStates";
import { getCategories } from "@/services/api/categoryApi";
import { getCostumes } from "@/services/api/costumeApi";
import { getApiErrorMessage } from "@/services/api/apiClient";

export default function StoreHomePage() {
  const [categories, setCategories] = useState([]);
  const [costumes, setCostumes] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingCostumes, setLoadingCostumes] = useState(true);
  const [categoryError, setCategoryError] = useState("");
  const [costumeError, setCostumeError] = useState("");

  useEffect(() => {
    const categoriesController = new AbortController();
    const costumesController = new AbortController();
    getCategories({ page: 0, size: 4, signal: categoriesController.signal })
      .then((data) => setCategories(data.content || []))
      .catch((error) => {
        if (!categoriesController.signal.aborted) setCategoryError(getApiErrorMessage(error, "Categories are temporarily unavailable."));
      })
      .finally(() => {
        if (!categoriesController.signal.aborted) setLoadingCategories(false);
      });
    getCostumes({ page: 0, size: 8, signal: costumesController.signal })
      .then((data) => setCostumes(data.content || []))
      .catch((error) => {
        if (!costumesController.signal.aborted) setCostumeError(getApiErrorMessage(error, "Costumes are temporarily unavailable."));
      })
      .finally(() => {
        if (!costumesController.signal.aborted) setLoadingCostumes(false);
      });
    return () => {
      categoriesController.abort();
      costumesController.abort();
    };
  }, []);

  return (
    <>
      <section className="store-hero">
        <div className="store-hero-image" role="img" aria-label="Editorial fashion portrait" />
        <div className="store-hero-overlay" />
        <div className="store-hero-content">
          <span className="store-eyebrow store-eyebrow-light">DRESS FOR YOUR MOMENT</span>
          <h1>Find your<br /><em>perfect look.</em></h1>
          <p>Discover thoughtfully selected occasionwear for weddings, celebrations, and every beautiful moment between.</p>
          <div className="store-hero-actions">
            <Link className="store-button store-button-light" href="/costumes">Browse collection <span>↗</span></Link>
            <Link className="store-text-cta" href="/try-on">Prepare for a virtual try-on <span>→</span></Link>
          </div>
          <span className="store-hero-footnote">Virtual try-on preparation is available; AI processing is coming soon.</span>
        </div>
        <div className="store-hero-caption"><span>THE OCCASION EDIT</span><span>01 — 04</span></div>
      </section>

      <section className="store-section store-section-spaced">
        <div className="store-section-heading">
          <StorePageHeading eyebrow="A WORLD OF OCCASIONS" title="Find your kind of celebration" description="Explore the collections available from our boutique." />
          <Link className="store-inline-link" href="/categories">All categories <span>↗</span></Link>
        </div>
        {loadingCategories ? <StoreLoading label="Loading categories…" /> :
          categoryError ? <StoreError message={categoryError} onRetry={() => window.location.reload()} /> :
            categories.length ? <div className="store-category-grid">{categories.map((category) => <CategoryCard category={category} key={category.id} />)}</div> :
              <StoreEmpty title="Categories are being curated" message="There are no active categories to display yet." />}
      </section>

      <section className="store-section store-featured-section">
        <div className="store-section-heading">
          <StorePageHeading eyebrow="THE BOUTIQUE SELECTION" title="Pieces for the occasion" description="A selection of currently available designer costumes." />
          <Link className="store-inline-link" href="/costumes">View all costumes <span>↗</span></Link>
        </div>
        {loadingCostumes ? <StoreLoading /> :
          costumeError ? <StoreError message={costumeError} onRetry={() => window.location.reload()} /> :
            costumes.length ? <CostumeGrid costumes={costumes} /> :
              <StoreEmpty title="The collection is on its way" message="No active costumes are available to show right now." />}
      </section>

      <section className="store-values-section">
        <div className="store-values-intro">
          <span className="store-eyebrow">THE STUDIO DIFFERENCE</span>
          <h2>Considered style.<br /><em>Personal moments.</em></h2>
          <p>Our team brings together a carefully selected edit of designer occasionwear, with a personal touch at every step.</p>
          <Link className="store-inline-link" href="/contact">Meet the boutique <span>↗</span></Link>
        </div>
        <div className="store-value-list">
          <article><span>01</span><div><h3>Curated with care</h3><p>Explore the boutique’s active catalog, selected for special occasions.</p></div></article>
          <article><span>02</span><div><h3>Discover your details</h3><p>See available colors, sizes, and current variant information on each piece.</p></div></article>
          <article><span>03</span><div><h3>A new way to preview</h3><p>Prepare your photo and selected look for a future virtual try-on experience.</p></div></article>
        </div>
      </section>

      <section className="store-tryon-banner">
        <div>
          <span className="store-eyebrow store-eyebrow-light">A NEW WAY TO EXPLORE</span>
          <h2>Imagine your next<br /><em>special moment.</em></h2>
          <p>Prepare a photo and select a costume. AI-powered previews are not available yet.</p>
          <Link className="store-button store-button-light" href="/try-on">Explore try-on preparation <span>↗</span></Link>
        </div>
        <span className="store-tryon-ornament" aria-hidden="true">DS</span>
      </section>

      <section className="store-contact-strip">
        <span className="store-eyebrow">A PERSONAL TOUCH</span>
        <h2>We’d love to help you find the one.</h2>
        <p>Get in touch with the boutique. Online inquiry submission is not available yet.</p>
        <Link className="store-button store-button-dark" href="/contact">Contact the boutique <span>↗</span></Link>
      </section>
    </>
  );
}
