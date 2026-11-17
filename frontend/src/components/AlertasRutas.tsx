"use client";

import Link from "next/link";
import { Etiqueta, Panel } from "@/components/ui";
import { useRecurso } from "@/lib/hooks";
import type { AlertaRuta } from "@/lib/tipos";

/** Las alertas dependen de la hora; se recalculan cada 30 segundos. */
export const REFRESCO_ALERTAS_MS = 30000;

/** ON-4: rutas en curso proximas a terminar su franja horaria sin completar entregas. */
export function AlertasRutas() {
  const { datos } = useRecurso<AlertaRuta[]>("/api/alertas/rutas", REFRESCO_ALERTAS_MS);
  const alertas = datos ?? [];
  if (!alertas.length) return null;
  const criticas = alertas.filter((a) => a.nivel === "CRITICA").length;

  return (
    <Panel titulo="Alertas de rutas próximas a finalizar"
      extra={<>
        <Etiqueta tono={criticas ? "danger" : "warn"}>{criticas ? `${criticas} crítica(s)` : `${alertas.length} advertencia(s)`}</Etiqueta>
        <Link className="enlace" href="/panel/alertas/">Ver todas</Link>
      </>}>
      <div className="panel-cuerpo" aria-live="polite">
        {alertas.map((a) => <AvisoAlerta key={a.rutaId} alerta={a} enlace />)}
      </div>
    </Panel>
  );
}

/** Aviso de una alerta: rojo si es critica, amarillo si es advertencia. */
export function AvisoAlerta({ alerta, enlace }: { alerta: AlertaRuta; enlace?: boolean }) {
  const critica = alerta.nivel === "CRITICA";
  return (
    <div className={`aviso ${critica ? "error" : "warn"}`} role={critica ? "alert" : "status"}>
      <strong>{critica ? "Crítica" : "Advertencia"}</strong>
      <span>{alerta.mensaje}.</span>
      {enlace ? <Link className="enlace" href={`/panel/rutas/detalle/?id=${alerta.rutaId}`}>Ver ruta {alerta.codigoRuta}</Link> : null}
    </div>
  );
}
