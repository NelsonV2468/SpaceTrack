"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { useAvisos } from "@/components/Avisos";
import { BarraEntregas, Cargando, Doble, Encabezado, Etiqueta, Kpi, Leyenda, Panel, Vacio } from "@/components/ui";
import { api } from "@/lib/api";
import { fecha, fechaHora, hoy, miles } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import { useSesion } from "@/lib/sesion";
import type { CierreDiario } from "@/lib/tipos";

/** ON-20 (epica ON-3 Seguimiento de rutas): cierre diario de rutas. */
export default function PaginaCierres() {
  const avisar = useAvisos();
  const { puedeEscribir } = useSesion();
  const [dia, setDia] = useState(hoy(0));
  const [enviando, setEnviando] = useState(false);
  const resumen = useRecurso<CierreDiario>(dia ? `/api/cierres/${dia}` : null);
  const historial = useRecurso<CierreDiario[]>("/api/cierres");

  async function cerrarDia(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const r = resumen.datos;
    if (!r) return;
    const observaciones = String(new FormData(e.currentTarget).get("observaciones") ?? "").trim();
    const aviso = r.entregasSinRegistrar + r.entregasNoRealizadas > 0
      ? ` Quedan ${r.entregasSinRegistrar} entregas sin registrar y ${r.entregasNoRealizadas} no entregadas.` : "";
    if (!confirm(`¿Cerrar el día ${fecha(dia)} con ${r.rutasCerradas} ruta(s)?${aviso} Después no se podrán iniciar ni finalizar rutas en ese día.`)) return;
    setEnviando(true);
    try {
      await api("POST", "/api/cierres", { fecha: dia, observaciones: observaciones || null });
      avisar("exito", `El día ${fecha(dia)} quedó cerrado.`);
    } catch (error) {
      avisar("error", (error as Error).message);
    } finally {
      setEnviando(false);
    }
    await Promise.all([resumen.recargar(), historial.recargar()]);
  }

  const r = resumen.datos;

  return (
    <>
      <Encabezado titulo="Cierre diario de rutas"
        subtitulo="Consolide las rutas finalizadas en el día. Después del cierre no se inician ni finalizan rutas en esa fecha." />

      <Panel titulo="Día a revisar" extra={
        <input type="date" aria-label="Fecha del cierre" value={dia} max={hoy(0)} style={{ width: "auto" }}
          onChange={(e) => setDia(e.target.value)} />
      }>
        <div className="panel-cuerpo">
          {!r ? (resumen.error ? <div className="aviso error" role="alert">{resumen.error}</div> : <Cargando />) : (
            <>
              {r.cerrado ? (
                <div className="aviso info">
                  <strong>Día cerrado</strong>
                  <span>el {fechaHora(r.fechaRegistro)} por {r.registradoPor}.{r.observaciones ? ` Observaciones: ${r.observaciones}` : ""}</span>
                </div>
              ) : r.puedeCerrar ? (
                <div className="aviso exito">Vista previa: el día {fecha(r.fecha)} está abierto y se puede cerrar.</div>
              ) : (
                <div className="aviso warn" role="status">
                  <strong>No se puede cerrar todavía.</strong>
                  <span>{r.motivoNoCierre}</span>
                </div>
              )}

              <section className="kpis">
                <Kpi titulo="Rutas del día" valor={r.rutasCerradas} nota={r.cerrado ? "Incluidas en el cierre" : "Finalizadas en la fecha"} acento="#16355a" />
                <Kpi titulo="Cumplimiento" valor={`${r.cumplimiento}%`} nota={`${miles(r.entregasRealizadas)} de ${miles(r.entregasProgramadas)} entregas`} acento="#0f7b4f" />
                <Kpi titulo="No entregadas" valor={r.entregasNoRealizadas} nota="Con novedad registrada" acento="#b3261e" />
                <Kpi titulo="Sin registrar" valor={r.entregasSinRegistrar} nota="Programadas sin resultado" acento="#9a5b00" />
              </section>

              {r.rutas.length ? (
                <div className="tabla">
                  <table>
                    <thead><tr><th>Ruta</th><th>Zona</th><th>Vehículo y conductor</th><th>Horario real</th><th>Entregas</th><th>Sin registrar</th></tr></thead>
                    <tbody>
                      {r.rutas.map((ruta) => (
                        <tr key={ruta.rutaId}>
                          <td data-titulo="Ruta">
                            <Doble principal={<Link className="enlace" href={`/panel/rutas/detalle/?id=${ruta.rutaId}`}>{ruta.codigoRuta}</Link>} secundario={ruta.nombreRuta ?? undefined} />
                          </td>
                          <td data-titulo="Zona">{ruta.nombreZona ?? "—"}</td>
                          <td data-titulo="Vehículo"><Doble principal={ruta.placaVehiculo ?? "—"} secundario={ruta.nombreConductor ?? undefined} /></td>
                          <td className="num" data-titulo="Horario">{`${ruta.fechaInicioReal?.slice(11, 16) ?? "—"} – ${ruta.fechaFinReal?.slice(11, 16) ?? "—"}`}</td>
                          <td data-titulo="Entregas">
                            <BarraEntregas realizadas={ruta.entregasRealizadas} noRealizadas={ruta.entregasNoRealizadas} programadas={ruta.entregasProgramadas} />
                          </td>
                          <td data-titulo="Sin registrar">
                            {ruta.entregasSinRegistrar ? <Etiqueta tono="warn">{ruta.entregasSinRegistrar}</Etiqueta> : <Etiqueta tono="ok">0</Etiqueta>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <Leyenda />
                </div>
              ) : <Vacio titulo="Sin rutas finalizadas" texto="No hay rutas finalizadas en esta fecha." />}

              {!r.cerrado && puedeEscribir ? (
                <form className="formulario espacio-arriba" onSubmit={cerrarDia}>
                  <div className="campo ancho">
                    <label htmlFor="observaciones">Observaciones del cierre (opcional)</label>
                    <textarea id="observaciones" name="observaciones" maxLength={255} placeholder="Ej.: lluvia en la tarde retrasó las entregas del sur" />
                  </div>
                  <div className="acciones-form">
                    <button type="submit" className="boton primario" disabled={!r.puedeCerrar || enviando}
                      title={r.puedeCerrar ? undefined : r.motivoNoCierre ?? undefined}>
                      {enviando ? "Cerrando…" : `Cerrar el día ${fecha(r.fecha)}`}
                    </button>
                  </div>
                </form>
              ) : null}
            </>
          )}
        </div>
      </Panel>

      <Panel titulo="Historial de cierres" extra={<span className="total">{historial.datos?.length ?? 0} cierres</span>}>
        {historial.datos?.length ? (
          <div className="tabla">
            <table>
              <thead><tr><th>Fecha</th><th>Rutas</th><th>Cumplimiento</th><th>No entregadas / sin registrar</th><th>Cerrado por</th><th className="derecha">Detalle</th></tr></thead>
              <tbody>
                {historial.datos.map((c) => (
                  <tr key={c.fecha}>
                    <td data-titulo="Fecha"><Doble principal={fecha(c.fecha)} secundario={`Registrado ${fechaHora(c.fechaRegistro)}`} /></td>
                    <td className="num" data-titulo="Rutas">{c.rutasCerradas}</td>
                    <td data-titulo="Cumplimiento">
                      <Doble principal={`${c.cumplimiento}%`} secundario={`${miles(c.entregasRealizadas)} de ${miles(c.entregasProgramadas)}`} />
                    </td>
                    <td className="num" data-titulo="Pendientes">{c.entregasNoRealizadas} / {c.entregasSinRegistrar}</td>
                    <td data-titulo="Cerrado por">{c.registradoPor}</td>
                    <td className="derecha" data-titulo="Detalle">
                      <button type="button" className="boton secundario chico" onClick={() => setDia(c.fecha)}>Ver</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <Vacio titulo="Aún no hay cierres" texto="Los días cerrados aparecerán aquí." />}
      </Panel>
    </>
  );
}
