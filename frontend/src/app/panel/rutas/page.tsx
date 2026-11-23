"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { Cargando } from "@/components/ui";
import { ModuloCrud } from "@/modulos/ModuloCrud";

// Pagina propia (no [modulo]) porque la carpeta rutas/ tambien contiene el detalle.
// Con ?operacion=ID (boton «Crear ruta» de Operaciones) abre el registro con ese contrato ya elegido.
export default function PaginaRutas() {
  return <Suspense fallback={<Cargando />}><Rutas /></Suspense>;
}

function Rutas() {
  const operacion = Number(useSearchParams().get("operacion")) || undefined;
  return <ModuloCrud clave="rutas" nuevoCon={operacion ? { operacion } : undefined} />;
}
