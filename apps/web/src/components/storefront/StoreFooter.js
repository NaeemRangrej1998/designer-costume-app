import Link from "next/link";

export default function StoreFooter() {
  const email = process.env.NEXT_PUBLIC_SHOP_EMAIL;
  const phone = process.env.NEXT_PUBLIC_SHOP_PHONE;

  return (
    <footer className="store-footer">
      <div className="store-footer-main">
        <div className="store-footer-brand">
          <Link className="store-brand store-brand-footer" href="/">
            <span className="store-brand-mark">DS</span>
            <span><strong>Designer Costume</strong><small>STUDIO</small></span>
          </Link>
          <p>Considered occasionwear, selected for the moments that matter.</p>
        </div>
        <div className="store-footer-column">
          <span className="store-footer-label">DISCOVER</span>
          <Link href="/categories">Categories</Link>
          <Link href="/costumes">Collections</Link>
          <Link href="/try-on">Virtual try-on</Link>
        </div>
        <div className="store-footer-column">
          <span className="store-footer-label">VISIT OR CONTACT</span>
          {email ? <a href={`mailto:${email}`}>{email}</a> : <span>Contact details not configured</span>}
          {phone ? <a href={`tel:${phone}`}>{phone}</a> : null}
          <Link href="/contact">Contact the boutique</Link>
        </div>
      </div>
      <div className="store-footer-bottom">
        <span>© {new Date().getFullYear()} Designer Costume Studio</span>
        <span>Made for memorable occasions</span>
      </div>
    </footer>
  );
}
