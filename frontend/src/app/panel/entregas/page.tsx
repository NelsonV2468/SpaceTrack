"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Icono } from "@/components/Icono";
import { BarraEntregas, Cargando, Doble, Encabezado, EstadoRutaEtiqueta, Kpi, Leyenda, Panel, Vacio } from "@/components/ui";
import { miles, porcentaje } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import { useSesion } from "@/lib/sesion";
import type { Entrega, Ruta } from "@/lib/tipos";
import { RegistroEntrega, TablaEntregas } from "@/modulos/entregas";

export default function PaginaEntregas() {
  const router = useRouter();
  const { puedeEscribir } = useSesion();
  const entregas = useRecurso<Entrega[]>("/api/entregas");
  const rutas = useRecurso<Ruta[]>("/api/rutas");
  const [registrando, setRegistrando] = useState(false);
  const [rutaFiltro, setRutaFiltro] = useState("");

  if (!entregas.datos || !rutas.datos) {
    const error = entregas.error ?? rutas.error;
    return error ? <Vacio titulo="No se pudo cargar la información" texto={error} /> : <Cargando />;
  }

  const recargar = async () => { await Promise.all([entregas.recargar(), rutas.recargar()]); };
  const iniciadas = rutas.datos.filter((r) => r.estado !== "PENDIENTE");
  const programadas = iniciadas.reduce((s, r) => s + r.numeroEntregasProgramadas, 0);
  const ok = entregas.datos.filter((e) => e.estado === "ENTREGADA").length;
  const novedad = entregas.datos.length - ok;
  const enCurso = new Set(rutas.datos.filter((r) => r.estado === "EN_CURSO").map((r) => r.id));
  const historial = rutaFiltro ? entregas.datos.filter((e) => e.rutaId === Number(rutaFiltro)) : entregas.datos;

  return (
    <>
      <Encabezado titulo="Registro de entregas" subtitulo="Entregas realizadas en campo frente a las programadas en cada ruta."
        acciones={puedeEscribir ? <button className="boton primario" onClick={() => setRegistrando(true)}><Icono nombre="mas" />Registrar entrega</button> : null} />

      <section className="kpis">
        <Kpi titulo="Programadas" valor={miles(programadas)} nota="En rutas iniciadas o finalizadas" acento="#16355a" />
        <Kpi titulo="Entregadas" valor={miles(ok)} nota={`${porcentaje(ok, programadas)}% de lo programado`} acento="#0f7b4f" />
        <Kpi titulo="No entregadas" valor={miles(novedad)} nota="Registradas con novedad" acento="#b3261e" />
        <Kpi titulo="Pendientes" valor={miles(Math.max(0, programadas - entregas.datos.length))} nota="Aún sin registrar" acento="#55627a" />
      </section>

      <Panel titulo="Cumplimiento por ruta" extra={<Leyenda />}>
        {iniciadas.length ? (
          <div className="tabla">
            <table>
              <thead><tr><th>Ruta</th><th>Estado</th><th>Programadas</th><th>Entregadas</th><th>No entregadas</th><th>Avance</th></tr></thead>
              <tbody>
                {iniciadas.map((r) => (
                  <tr key={r.id} className="clic" onClick={() => router.push(`/panel/rutas/detalle/?id=${r.id}`)}>
                    <td data-titulo="Ruta"><Doble principal={r.codigo} secundario={r.nombre} /></td>
                    <td data-titulo="Estado"><EstadoRutaEtiqueta estado={r.estado} /></td>
                    <td className="num" data-titulo="Programadas">{r.numeroEntregasProgramadas}</td>
                    <td className="num" data-titulo="Entregadas">{r.entregasRealizadas}</td>
                    <td className="num" data-titulo="No entregadas">{r.entregasNoRealizadas}</td>
                    <td data-titulo="Avance"><BarraEntregas realizadas={r.entregasRealizadas} noRealizadas={r.entregasNoRealizadas} programadas={r.numeroEntregasProgramadas} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <Vacio titulo="No hay rutas iniciadas" texto="Inicie una ruta desde su detalle para registrar entregas." />}
      </Panel>

      <Panel titulo="Historial de entregas" extra={
        <div className="leyenda">
          <select aria-label="Filtrar por ruta" value={rutaFiltro} onChange={(e) => setRutaFiltro(e.target.value)}>
            <option value="">Todas las rutas</option>
            {iniciadas.map((r) => <option key={r.id} value={r.id}>{r.codigo} · {r.nombre}</option>)}
          </select>
          <span className="total">{historial.length} registros</span>
        </div>
      }>
        <TablaEntregas entregas={historial} mostrarRuta puedeAnular={(e) => enCurso.has(e.rutaId)} alCambiar={recargar} />
      </Panel>

      {registrando ? (
        <RegistroEntrega rutasEnCurso={rutas.datos.filter((r) => r.estado === "EN_CURSO")}
          alCerrar={() => setRegistrando(false)} alGuardar={recargar} />
      ) : null}
    </>
  );
}
