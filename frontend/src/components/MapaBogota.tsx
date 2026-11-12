"use client";

import { useMemo } from "react";
import { svgMapa, type OpcionesMapa } from "@/lib/mapa";
import type { Ubicacion } from "@/lib/tipos";

/** Plano SVG de Bogota. El SVG se genera con todo el texto dinamico escapado (lib/mapa.ts). */
export function MapaBogota({ ubicaciones, opciones, mini, alSeleccionar }: {
  ubicaciones: Ubicacion[]; opciones?: OpcionesMapa; mini?: boolean; alSeleccionar?: (rutaId: number) => void;
}) {
  const svg = useMemo(() => svgMapa(ubicaciones, opciones), [ubicaciones, opciones]);
  return (
    <div
      className={`lienzo-mapa${mini ? " mini" : ""}`}
      onClick={(e) => {
        const nodo = (e.target as Element).closest("[data-ruta]");
        if (nodo && alSeleccionar) alSeleccionar(Number(nodo.getAttribute("data-ruta")));
      }}
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
}
