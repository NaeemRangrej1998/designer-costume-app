import "./globals.css";
import Providers from "@/components/providers";

export const metadata = {
  title: "Designer Costume Studio | Occasionwear",
  description: "Explore the designer costume boutique collection and prepare for a future virtual try-on.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body><Providers>{children}</Providers></body>
    </html>
  );
}
