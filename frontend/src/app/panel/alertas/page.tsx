"use client";

import Link from "next/link";
import { useState } from "react";
import { REFRESCO_ALERTAS_MS } from "@/components/AlertasRutas";
import { Cargando, Doble, Encabezado, Etiqueta, Kpi, Panel, Vacio } from "@/components/ui";
import { hora } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import type { AlertaRuta, NivelAlerta } from "@/lib/tipos";

type Filtro = "TODAS" | NivelAlerta;

const FILTROS: [Filtro, string][] = [["TODAS", "Todas"], ["CRITICA", "Críticas"], ["ADVERTENCIA", "Advertencias"]];

/** Tiempo frente al cierre de la franja: restante, vencido o abierta desde otro dia. */
function tiempo(a: AlertaRuta): string {
  if (!a.horaFinFranja) return "Abierta desde un día anterior";
  if (a.minutosRestantes <= 0) return `Cerró hace ${Math.abs(a.minutosRestantes)} min (${hora(a.horaFinFranja)})`;
  return `${a.minutosRestantes} min (cierra ${hora(a.horaFinFranja)})`;
}

/** ON-4: pantalla de alertas de rutas proximas a finalizar sin completar entregas. */
export default function PaginaAlertas() {
  const [filtro, setFiltro] = useState<Filtro>("TODAS");
  const { datos, error, cargando } = useRecurso<AlertaRuta[]>("/api/alertas/rutas", REFRESCO_ALERTAS_MS);

  if (!datos) return error ? <Vacio titulo="No se pudieron cargar las alertas" texto={error} /> : cargando ? <Cargando /> : null;

  const criticas = datos.filter((a) => a.nivel === "CRITICA").length;
  const advertencias = datos.length - criticas;
  const pendientes = datos.reduce((s, a) => s + a.entregasPendientes, 0);
  const visibles = filtro === "TODAS" ? datos : datos.filter((a) => a.nivel === filtro);
  const actualizado = datos[0]?.calculadoEn;

  return (
    <>
      <Encabezado titulo="Alertas de rutas"
        subtitulo="Rutas en curso próximas a terminar su franja horaria sin haber completado sus entregas. Se actualiza cada 30 segundos." />

      <section className="kpis">
        <Kpi titulo="Alertas críticas" valor={criticas} nota="El tiempo no alcanza o la franja ya cerró" acento="#b3261e" />
        <Kpi titulo="Advertencias" valor={advertencias} nota="La franja cierra en menos de una hora" acento="#9a5b00" />
        <Kpi titulo="Entregas en riesgo" valor={pendientes} nota="Pendientes en las rutas con alerta" acento="#16355a" />
      </section>

      <Panel titulo="Rutas con alerta" extra={actualizado ? <span className="total">Actualizado {hora(actualizado.slice(11))}</span> : undefined}>
        <div className="herramientas">
          <div className="izq">
            <select aria-label="Filtrar por nivel" value={filtro} onChange={(e) => setFiltro(e.target.value as Filtro)}>
              {FILTROS.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
            </select>
          </div>
          <span className="total">{visibles.length} {visibles.length === 1 ? "alerta" : "alertas"}</span>
        </div>
        {visibles.length ? (
          <div className="tabla">
            <table>
              <thead>
                <tr><th>Nivel</th><th>Ruta</th><th>Zona / contrato</th><th>Vehículo y conductor</th><th>Entregas pendientes</th><th>Tiempo de la franja</th><th className="derecha">Acción</th></tr>
              </thead>
              <tbody>
                {visibles.map((a) => (
                  <tr key={a.rutaId} title={a.mensaje}>
                    <td data-titulo="Nivel">
                      <Etiqueta tono={a.nivel === "CRITICA" ? "danger" : "warn"}>{a.nivel === "CRITICA" ? "Crítica" : "Advertencia"}</Etiqueta>
                    </td>
                    <td data-titulo="Ruta"><Doble principal={a.codigoRuta} secundario={a.nombreRuta} /></td>
                    <td data-titulo="Zona"><Doble principal={a.nombreZona ?? "—"} secundario={[a.localidad, a.codigoContrato].filter(Boolean).join(" · ")} /></td>
                    <td data-titulo="Vehículo"><Doble principal={a.placaVehiculo ?? "—"} secundario={a.nombreConductor ?? undefined} /></td>
                    <td className="num" data-titulo="Pendientes">
                      <Doble principal={`${a.entregasPendientes} de ${a.entregasProgramadas}`} secundario={`≈ ${a.minutosEstimadosPendientes} min estimados`} />
                    </td>
                    <td className="num" data-titulo="Franja">{tiempo(a)}</td>
                    <td className="derecha" data-titulo="Acción">
                      <Link className="boton secundario chico" href={`/panel/rutas/detalle/?id=${a.rutaId}`}>Ver ruta</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Vacio titulo={datos.length ? "No hay alertas con este filtro" : "Sin alertas"}
            texto={datos.length ? "Cambie el filtro para ver las demás." : "Ninguna ruta en curso está próxima a terminar su franja con entregas pendientes."} />
        )}
      </Panel>
    </>
  );
}
