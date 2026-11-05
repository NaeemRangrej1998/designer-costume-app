import StoreHeader from "./StoreHeader";
import StoreFooter from "./StoreFooter";

export default function StoreLayout({ children }) {
  return (
    <div className="storefront">
      <StoreHeader />
      <main>{children}</main>
      <StoreFooter />
    </div>
  );
}
