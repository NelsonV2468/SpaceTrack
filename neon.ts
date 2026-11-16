import { defineConfig } from "@neon/config/v1";

// Solo los servicios incluidos en el plan gratuito de Neon: Postgres (implicito) y Neon Auth.
// Functions, AI Gateway y Realtime se retiraron: SpaceTrack no los usa y son los que piden creditos.
export default defineConfig({
  auth: true,
  branch: (branch) => {
    if (branch.isDefault) return {};
    // Ramas nuevas de prueba: expiran solas para no consumir la cuota gratuita.
    if (!branch.exists) return { ttl: "7d" };
    return {};
  },
});
