import { ModuloCrud } from "@/modulos/ModuloCrud";

// Pagina propia (no [modulo]) porque la carpeta rutas/ tambien contiene el detalle.
export default function PaginaRutas() {
  return <ModuloCrud clave="rutas" />;
}
