import Link from "next/link";
import StorePageHeading from "@/components/storefront/StorePageHeading";

export default function ContactPage() {
  const address = process.env.NEXT_PUBLIC_SHOP_ADDRESS;
  const phone = process.env.NEXT_PUBLIC_SHOP_PHONE;
  const email = process.env.NEXT_PUBLIC_SHOP_EMAIL;

  return (
    <div className="store-page-container">
      <StorePageHeading eyebrow="WE’RE HERE TO HELP" title="Visit or get in touch" description="For personal styling, sizing questions, or details about a costume, contact the boutique directly." />
      <div className="contact-layout">
        <section className="contact-details-card">
          <span className="store-eyebrow">THE BOUTIQUE</span>
          <h2>Designer Costume Studio</h2>
          <div className="contact-detail-list">
            <div><span>ADDRESS</span>{address || "Shop address not configured"}</div>
            <div><span>PHONE</span>{phone ? <a href={`tel:${phone}`}>{phone}</a> : "Phone number not configured"}</div>
            <div><span>EMAIL</span>{email ? <a href={`mailto:${email}`}>{email}</a> : "Email not configured"}</div>
          </div>
          <p>Store contact details can be configured through the web environment variables. No location or contact information is assumed.</p>
        </section>
        <section className="contact-form-card">
          <span className="store-eyebrow">SEND A MESSAGE</span>
          <h2>How can we help?</h2>
          <div className="contact-form">
            <label><span>Your name</span><input className="store-input" disabled placeholder="Name" /></label>
            <label><span>Email address</span><input className="store-input" disabled placeholder="Email" /></label>
            <label><span>Your message</span><textarea className="store-input" disabled rows="5" placeholder="Tell us what you’re looking for" /></label>
            <button type="button" className="store-button store-button-dark" disabled>Inquiry submission unavailable</button>
            <p>The backend does not currently have an inquiry submission API. Your message will not be sent or saved.</p>
            {email && <Link className="store-inline-link" href={`mailto:${email}`}>Email the boutique directly <span>↗</span></Link>}
          </div>
        </section>
      </div>
    </div>
  );
}
