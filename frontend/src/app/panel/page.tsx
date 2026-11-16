"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Icono } from "@/components/Icono";
import { BarraEntregas, Cargando, Doble, Encabezado, Etiqueta, EstadoRutaEtiqueta, Kpi, Leyenda, Panel, Vacio } from "@/components/ui";
import { licenciaVigente, miles, porcentaje } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import type { Conductor, Necesidad, Ruta, Vehiculo } from "@/lib/tipos";

export default function Tablero() {
  const router = useRouter();
  const rutas = useRecurso<Ruta[]>("/api/rutas");
  const vehiculos = useRecurso<Vehiculo[]>("/api/vehiculos");
  const conductores = useRecurso<Conductor[]>("/api/conductores");
  const necesidades = useRecurso<Necesidad[]>("/api/necesidades");

  if (!rutas.datos && rutas.cargando) return <Cargando />;
  if (rutas.error && !rutas.datos) return <Vacio titulo="No se pudo cargar el tablero" texto={rutas.error} />;

  const listaRutas = rutas.datos ?? [];
  const listaVehiculos = vehiculos.datos ?? [];
  const listaConductores = conductores.datos ?? [];
  const cuenta = (e: Ruta["estado"]) => listaRutas.filter((r) => r.estado === e).length;
  const iniciadas = listaRutas.filter((r) => r.estado !== "PENDIENTE");
  const programadas = iniciadas.reduce((s, r) => s + r.numeroEntregasProgramadas, 0);
  const realizadas = iniciadas.reduce((s, r) => s + r.entregasRealizadas, 0);
  const noRealizadas = iniciadas.reduce((s, r) => s + r.entregasNoRealizadas, 0);
  const habilitados = listaConductores.filter((c) => c.activo && licenciaVigente(c)).length;
  const sinRecursos = listaRutas.filter((r) => r.estado === "PENDIENTE" && !r.placaVehiculo).length;
  const sinFlota = (necesidades.datos ?? []).filter((n) => !n.flotaSuficiente).length;
  const activas = listaRutas.filter((r) => r.estado !== "FINALIZADA");

  return (
    <>
      <Encabezado titulo="Tablero de control" subtitulo="Resumen de la operación de distribución."
        acciones={<>
          <Link className="boton secundario" href="/panel/monitoreo/"><Icono nombre="monitoreo" />Ver mapa</Link>
          <Link className="boton primario" href="/panel/rutas/"><Icono nombre="rutas" />Gestionar rutas</Link>
        </>} />

      <section className="kpis">
        <Kpi titulo="Rutas en curso" valor={cuenta("EN_CURSO")} nota={`de ${listaRutas.length} rutas registradas`} acento="#1d5fbf" />
        <Kpi titulo="Rutas pendientes" valor={cuenta("PENDIENTE")} nota={sinRecursos ? `${sinRecursos} sin vehículo o conductor` : "Todas con recursos asignados"}
          acento={sinRecursos ? "#9a5b00" : "#55627a"} />
        <Kpi titulo="Rutas finalizadas" valor={cuenta("FINALIZADA")} nota="Cerradas en el sistema" acento="#0f7b4f" />
        <Kpi titulo="Cumplimiento de entregas" valor={`${porcentaje(realizadas, programadas)}%`}
          nota={`${miles(realizadas)} de ${miles(programadas)} programadas${noRealizadas ? ` · ${noRealizadas} con novedad` : ""}`} acento="#0f7b4f" />
        <Kpi titulo="Flota activa" valor={listaVehiculos.filter((v) => v.activo).length} nota={`de ${listaVehiculos.length} vehículos`} acento="#16355a" />
        <Kpi titulo="Conductores habilitados" valor={habilitados} nota={`de ${listaConductores.length} registrados`} acento="#16355a" />
      </section>

      {sinFlota ? (
        <div className="aviso warn">
          <span>{sinFlota} necesidad(es) de cliente no tienen suficientes vehículos compatibles en la flota.</span>
          <Link className="enlace" href="/panel/necesidades/">Revisar</Link>
        </div>
      ) : null}

      <Panel titulo="Rutas activas" extra={<Leyenda />}>
        {activas.length ? (
          <div className="tabla">
            <table>
              <thead><tr><th>Ruta</th><th>Zona</th><th>Vehículo / conductor</th><th>Entregas</th><th>Estado</th></tr></thead>
              <tbody>
                {activas.map((r) => (
                  <tr key={r.id} className="clic" onClick={() => router.push(`/panel/rutas/detalle/?id=${r.id}`)}>
                    <td data-titulo="Ruta"><Doble principal={r.codigo} secundario={r.nombre} /></td>
                    <td data-titulo="Zona"><Doble principal={r.nombreZona} secundario={r.codigoContrato} /></td>
                    <td data-titulo="Vehículo">{r.placaVehiculo ? <Doble principal={r.placaVehiculo} secundario={r.nombreConductor} /> : <Etiqueta tono="warn">Sin asignar</Etiqueta>}</td>
                    <td data-titulo="Entregas"><BarraEntregas realizadas={r.entregasRealizadas} noRealizadas={r.entregasNoRealizadas} programadas={r.numeroEntregasProgramadas} /></td>
                    <td data-titulo="Estado"><EstadoRutaEtiqueta estado={r.estado} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <Vacio titulo="No hay rutas activas" texto="Las rutas pendientes y en curso aparecerán aquí." />}
      </Panel>
    </>
  );
}
