"use client";

import Link from "next/link";
import { useState } from "react";
import { REFRESCO_ALERTAS_MS, tituloAlerta } from "@/components/AlertasRutas";
import { Cargando, Doble, Encabezado, Etiqueta, Kpi, Panel, Vacio } from "@/components/ui";
import { hora } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import type { AlertaRuta } from "@/lib/tipos";

type Filtro = "TODAS" | "CRITICA" | "ADVERTENCIA" | "DESVIO_RUTA" | "FRANJA_HORARIA";

const FILTROS: [Filtro, string][] = [
  ["TODAS", "Todas"], ["CRITICA", "Solo críticas"], ["ADVERTENCIA", "Solo advertencias"],
  ["DESVIO_RUTA", "Vehículos fuera de ruta"], ["FRANJA_HORARIA", "Entregas fuera de tiempo"],
];

/** Detalle de la situacion: distancia al recorrido o tiempo frente al cierre de la franja. */
function situacion(a: AlertaRuta): string {
  if (a.tipo === "DESVIO_RUTA") return `A ${(a.distanciaDesvioKm ?? 0).toLocaleString("es-CO")} km del recorrido`;
  if (!a.horaFinFranja) return "Abierta desde un día anterior";
  if (a.minutosRestantes <= 0) return `Franja cerró hace ${Math.abs(a.minutosRestantes)} min (${hora(a.horaFinFranja)})`;
  return `Quedan ${a.minutosRestantes} min (cierra ${hora(a.horaFinFranja)})`;
}

const coincide = (a: AlertaRuta, f: Filtro) => f === "TODAS" || a.nivel === f || a.tipo === f;

/** Alertas de rutas en curso: entregas en riesgo por la franja horaria (ON-4) y vehiculos desviados de su ruta. */
export default function PaginaAlertas() {
  const [filtro, setFiltro] = useState<Filtro>("TODAS");
  const { datos, error, cargando } = useRecurso<AlertaRuta[]>("/api/alertas/rutas", REFRESCO_ALERTAS_MS);

  if (!datos) return error ? <Vacio titulo="No se pudieron cargar las alertas" texto={error} /> : cargando ? <Cargando /> : null;

  const criticas = datos.filter((a) => a.nivel === "CRITICA").length;
  const desviados = datos.filter((a) => a.tipo === "DESVIO_RUTA").length;
  const pendientes = datos.filter((a) => a.tipo === "FRANJA_HORARIA").reduce((s, a) => s + a.entregasPendientes, 0);
  const visibles = datos.filter((a) => coincide(a, filtro));
  const actualizado = datos[0]?.calculadoEn;

  return (
    <>
      <Encabezado titulo="Alertas de rutas"
        subtitulo="Rutas en curso que necesitan atención: vehículos fuera de su recorrido o entregas que no alcanzan a completarse en la franja horaria. Se actualiza cada 30 segundos." />

      <section className="kpis">
        <Kpi titulo="Alertas críticas" valor={criticas} nota="Requieren acción inmediata" acento="#b3261e" />
        <Kpi titulo="Vehículos fuera de ruta" valor={desviados} nota="Se alejaron de su recorrido planeado" acento="#9a5b00" />
        <Kpi titulo="Entregas en riesgo" valor={pendientes} nota="Pendientes en rutas con poco tiempo" acento="#16355a" />
      </section>

      <Panel titulo="Rutas con alerta" extra={actualizado ? <span className="total">Actualizado {hora(actualizado.slice(11))}</span> : undefined}>
        <div className="herramientas">
          <div className="izq">
            <select aria-label="Filtrar alertas" value={filtro} onChange={(e) => setFiltro(e.target.value as Filtro)}>
              {FILTROS.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
            </select>
          </div>
          <span className="total">{visibles.length} {visibles.length === 1 ? "alerta" : "alertas"}</span>
        </div>
        {visibles.length ? (
          <div className="tabla">
            <table>
              <thead>
                <tr><th>Alerta</th><th>Ruta</th><th>Zona / contrato</th><th>Vehículo y conductor</th><th>Entregas pendientes</th><th>Situación</th><th className="derecha">Acción</th></tr>
              </thead>
              <tbody>
                {visibles.map((a) => (
                  <tr key={`${a.rutaId}-${a.tipo}`} title={a.mensaje}>
                    <td data-titulo="Alerta">
                      <Doble principal={<Etiqueta tono={a.nivel === "CRITICA" ? "danger" : "warn"}>{tituloAlerta(a)}</Etiqueta>}
                        secundario={a.nivel === "CRITICA" ? "Crítica" : "Advertencia"} />
                    </td>
                    <td data-titulo="Ruta"><Doble principal={a.codigoRuta} secundario={a.nombreRuta} /></td>
                    <td data-titulo="Zona"><Doble principal={a.localidad ?? a.nombreZona ?? "—"} secundario={a.codigoContrato ?? undefined} /></td>
                    <td data-titulo="Vehículo"><Doble principal={a.placaVehiculo ?? "—"} secundario={a.nombreConductor ?? undefined} /></td>
                    <td className="num" data-titulo="Pendientes">
                      <Doble principal={`${a.entregasPendientes} de ${a.entregasProgramadas}`} secundario={`≈ ${a.minutosEstimadosPendientes} min estimados`} />
                    </td>
                    <td className="num" data-titulo="Situación">{situacion(a)}</td>
                    <td className="derecha" data-titulo="Acción">
                      <Link className="boton secundario chico" href={`/panel/rutas/detalle/?id=${a.rutaId}`}>Ver ruta</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Vacio titulo={datos.length ? "No hay alertas con este filtro" : "Todo en orden"}
            texto={datos.length ? "Cambie el filtro para ver las demás." : "Ningún vehículo está fuera de ruta y todas las rutas en curso van a tiempo."} />
        )}
      </Panel>
    </>
  );
}
