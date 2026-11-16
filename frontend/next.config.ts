import type { NextConfig } from "next";

const produccion = process.env.NODE_ENV === "production";
// Backend Spring Boot usado por "npm run dev" (perfil dev, puerto 8080).
const backend = process.env.SPACETRACK_BACKEND ?? "http://localhost:8080";

/**
 * En produccion la app se exporta como HTML/JS estatico y la sirve Spring Boot desde el mismo
 * dominio: asi la cookie de sesion HttpOnly y el token CSRF funcionan sin CORS.
 * En desarrollo, Next reenvia /api y /auth al backend para simular ese mismo origen.
 */
const config: NextConfig = {
  output: produccion ? "export" : undefined,
  trailingSlash: true,
  // En dev no se agrega "/" a /api y /auth antes de reenviarlos al backend.
  skipTrailingSlashRedirect: true,
  images: { unoptimized: true },
  poweredByHeader: false,
  reactStrictMode: true,
  // El repositorio tiene otro package-lock.json en la raiz (neon.ts); la app vive en frontend/.
  turbopack: { root: __dirname },
  ...(produccion
    ? {}
    : {
        async rewrites() {
          return [
            { source: "/api/:ruta*", destination: `${backend}/api/:ruta*` },
            { source: "/auth/:ruta*", destination: `${backend}/auth/:ruta*` },
          ];
        },
      }),
};

export default config;
