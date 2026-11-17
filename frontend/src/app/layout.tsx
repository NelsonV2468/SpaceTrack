import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "SpaceTrack", template: "%s | SpaceTrack" },
  description: "Centro de control de rutas de distribución de City Urban Bogotá.",
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#f5f5f7",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    // Algunas extensiones del navegador agregan atributos a <html> antes de que cargue React
    // (por ejemplo jd-enabled); se ignora esa diferencia solo en esta etiqueta.
    <html lang="es" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
