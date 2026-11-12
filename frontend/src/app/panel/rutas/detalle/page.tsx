"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState, type FormEvent } from "react";
import { useAvisos } from "@/components/Avisos";
import { Formulario } from "@/components/Formulario";
import { Icono, type NombreIcono } from "@/components/Icono";
import { MapaBogota } from "@/components/MapaBogota";
import { Modal } from "@/components/Modal";
import { BarraEntregas, Cargando, Encabezado, Etiqueta, Leyenda, Panel, Vacio } from "@/components/ui";
import { api, apiGet, ApiError } from "@/lib/api";
import { ESTADOS_RUTA, INICIAL_DIA, REFRESCO_MAPA_MS, fechaHora, hora, licenciaVigente, miles, nombreConductor, porcentaje } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import { mensajeSinUbicacion } from "@/lib/mapa";
import { useSesion } from "@/lib/sesion";
import type { Conductor, Entrega, Necesidad, Operacion, Ruta, Ubicacion, Vehiculo, Zona } from "@/lib/tipos";
import { CAMPOS_RUTA } from "@/modulos/config";
import { RegistroEntrega, TablaEntregas } from "@/modulos/entregas";

export default function PaginaDetalle() {
  return <Suspense fallback={<Cargando />}><DetalleRuta /></Suspense>;
}

function DetalleRuta() {
  const id = Number(useSearchParams().get("id"));
  const router = useRouter();
  const avisar = useAvisos();
  const { puedeEscribir, esAdmin } = useSesion();
  const ruta = useRecurso<Ruta>(id ? `/api/rutas/${id}` : null);
  const entregas = useRecurso<Entrega[]>(id ? `/api/entregas?rutaId=${id}` : null);
  const enCurso = ruta.datos?.estado === "EN_CURSO";
  const ubicacion = useRecurso<Ubicacion>(id && ruta.datos ? `/api/rutas/${id}/ubicacion` : null, enCurso ? REFRESCO_MAPA_MS : undefined);
  const [modal, setModal] = useState<"editar" | "asignar" | "entrega" | null>(null);
  const [fuentesEdicion, setFuentesEdicion] = useState<Record<string, never[]>>({});
  const opcionesMapa = useMemo(() => ({ enfocar: true }), []);
  const ubicaciones = useMemo(() => (ubicacion.datos ? [ubicacion.datos] : []), [ubicacion.datos]);

  if (!id) return <Vacio titulo="Ruta no indicada" texto={<Link className="enlace" href="/panel/rutas/">Volver a rutas</Link>} />;
  if (!ruta.datos) return ruta.error ? <Vacio titulo="No se pudo cargar la ruta" texto={ruta.error} /> : <Cargando />;

  const r = ruta.datos;
  const recargar = async () => { await Promise.all([ruta.recargar(), entregas.recargar(), ubicacion.recargar()]); };
  const p = r.numeroEntregasProgramadas;
  const pendientes = p - r.entregasRealizadas - r.entregasNoRealizadas;
  const sinRecursos = !r.placaVehiculo || !r.conductorId;

  async function cambiarEstado(accion: "iniciar" | "finalizar") {
    if (accion === "finalizar") {
      const texto = pendientes > 0 ? `Quedan ${pendientes} entregas sin registrar. ¿Desea finalizar la ruta de todas formas?` : `¿Desea finalizar la ruta ${r.codigo}?`;
      if (!confirm(texto)) return;
    }
    try {
      await api("PATCH", `/api/rutas/${r.id}/${accion}`);
      avisar("exito", accion === "iniciar" ? "La ruta se inició. Ya puede registrar entregas." : "La ruta se finalizó correctamente.");
    } catch (e) {
      avisar("error", (e as Error).message);
    }
    await recargar();
  }

  async function eliminar() {
    if (!confirm("¿Desea eliminar esta ruta? Esta acción no se puede deshacer.")) return;
    try {
      await api("DELETE", `/api/rutas/${r.id}`);
      avisar("exito", "Ruta eliminada.");
      router.push("/panel/rutas/");
    } catch (e) {
      avisar("error", (e as Error).message);
    }
  }

  async function abrirEdicion() {
    const [zonas, operaciones] = await Promise.all([apiGet<Zona[]>("/api/zonas"), apiGet<Operacion[]>("/api/operaciones")]).catch(() => [[], []]);
    setFuentesEdicion({ zonas: zonas as never[], operaciones: operaciones as never[] });
    setModal("editar");
  }

  const orden = ["PENDIENTE", "EN_CURSO", "FINALIZADA"] as const;
  const actual = orden.indexOf(r.estado);
  const fechasEtapa = [`Creada ${fechaHora(r.fechaCreacion)}`, r.fechaInicioReal ? fechaHora(r.fechaInicioReal) : "Sin iniciar", r.fechaFinReal ? fechaHora(r.fechaFinReal) : "Sin cerrar"];
  const u = ubicacion.datos;
  const sinUbicacion = u ? mensajeSinUbicacion(u) : null;

  return (
    <>
      <Encabezado titulo={`${r.codigo} · ${r.nombre}`}
        subtitulo={`${r.nombreZona} · Contrato ${r.codigoContrato}${r.razonSocialCliente ? ` · ${r.razonSocialCliente}` : ""}`}
        volver={<Link className="volver" href="/panel/rutas/">← Rutas</Link>}
        acciones={<>
          {puedeEscribir && r.estado !== "FINALIZADA" ? <>
            <button className="boton secundario" onClick={abrirEdicion}>Editar</button>
            <button className="boton secundario" onClick={() => setModal("asignar")}>{sinRecursos ? "Asignar recursos" : "Cambiar recursos"}</button>
          </> : null}
          {esAdmin && r.estado === "PENDIENTE" ? <button className="boton peligro" onClick={eliminar}>Eliminar</button> : null}
          {puedeEscribir && r.estado === "PENDIENTE" ? (
            <button className="boton primario" disabled={sinRecursos} title={sinRecursos ? "Asigne vehículo y conductor para iniciar" : undefined}
              onClick={() => cambiarEstado("iniciar")}>Iniciar ruta</button>
          ) : null}
          {puedeEscribir && r.estado === "EN_CURSO" ? <>
            <button className="boton primario" disabled={pendientes <= 0} onClick={() => setModal("entrega")}><Icono nombre="mas" />Registrar entrega</button>
            <button className="boton exito" onClick={() => cambiarEstado("finalizar")}>Finalizar ruta</button>
          </> : null}
        </>} />

      {r.estado === "PENDIENTE" && sinRecursos ? (
        <div className="aviso warn">La ruta no tiene vehículo y conductor asignados. Asígnelos para poder iniciarla.</div>
      ) : null}

      <Panel>
        <div className="panel-cuerpo">
          <ol className="etapas">
            {orden.map((e, i) => {
              const clase = i < actual || r.estado === "FINALIZADA" ? "hecha" : i === actual ? "actual" : "";
              return (
                <li key={e} className={clase}>
                  <span className="circulo">{clase === "hecha" ? "✓" : i + 1}</span>
                  <span>{ESTADOS_RUTA[e][0]}<span className="fecha">{fechasEtapa[i]}</span></span>
                </li>
              );
            })}
          </ol>
        </div>
      </Panel>

      <div className="rejilla detalle">
        <div className="rejilla">
          <Panel titulo="Entregas realizadas frente a programadas" extra={<Leyenda />}>
            <div className="panel-cuerpo">
              <div className="cifras">
                <Cifra titulo="Programadas" valor={p} />
                <Cifra titulo="Entregadas" valor={r.entregasRealizadas} color="var(--ok)" />
                <Cifra titulo="No entregadas" valor={r.entregasNoRealizadas} color="var(--danger)" />
                <Cifra titulo="Pendientes" valor={Math.max(0, pendientes)} />
              </div>
              <BarraEntregas alta realizadas={r.entregasRealizadas} noRealizadas={r.entregasNoRealizadas} programadas={p}
                texto={`${porcentaje(r.entregasRealizadas, p)}% cumplido`} />
            </div>
            <TablaEntregas entregas={entregas.datos ?? []} puedeAnular={() => enCurso} alCambiar={recargar} />
          </Panel>

          <Panel titulo="Ubicación simulada del vehículo" extra={enCurso ? <Etiqueta tono="info">En vivo</Etiqueta> : null}>
            <MapaBogota mini ubicaciones={ubicaciones} opciones={opcionesMapa} />
            <div className="mapa-pie">
              {!u ? <span>{ubicacion.error ?? "Cargando ubicación…"}</span> : sinUbicacion ? <span>{sinUbicacion}</span> : <>
                <span>{u.descripcionPosicion}{u.velocidadKmh ? ` · ${u.velocidadKmh} km/h` : ""}</span>
                <span className="num">Avance {Math.round(u.progreso * 100)}% · {u.calculadoEn?.slice(11, 19)}</span>
              </>}
            </div>
          </Panel>
        </div>

        <div className="rejilla">
          <Panel titulo="Recursos asignados"
            extra={puedeEscribir && r.estado !== "FINALIZADA" ? <button className="enlace" onClick={() => setModal("asignar")}>{sinRecursos ? "Asignar" : "Cambiar"}</button> : null}>
            <div className="panel-cuerpo">
              <Recurso icono="vehiculos" titulo="Vehículo" principal={r.placaVehiculo} secundario={r.descripcionVehiculo} />
              <Recurso icono="conductores" titulo="Conductor" principal={r.nombreConductor} secundario="Conductor asignado" />
            </div>
          </Panel>
          <Panel titulo="Datos de la ruta">
            <div className="panel-cuerpo">
              <dl className="ficha">
                <Dato titulo="Origen" valor={r.puntoOrigen} />
                <Dato titulo="Destino" valor={r.puntoDestino} />
                <Dato titulo="Distancia estimada" valor={`${miles(r.distanciaEstimadaKm)} km`} />
                <Dato titulo="Entregas programadas" valor={miles(p)} />
                <Dato titulo="Zona" valor={r.nombreZona} />
                <Dato titulo="Contrato" valor={r.codigoContrato} />
                <Dato ancho titulo="Entregas del contrato"
                  valor={`${miles(r.entregasEsperadasContrato)} esperadas · ${miles(r.entregasProgramadasContrato)} programadas en sus rutas`} />
                <Dato titulo="Creada" valor={fechaHora(r.fechaCreacion)} />
                <Dato titulo="Actualizada" valor={fechaHora(r.fechaActualizacion)} />
              </dl>
              {r.entregasProgramadasContrato > r.entregasEsperadasContrato ? (
                <div className="aviso warn espacio-arriba">
                  Las rutas de este contrato programan más entregas ({miles(r.entregasProgramadasContrato)}) de las esperadas ({miles(r.entregasEsperadasContrato)}).
                </div>
              ) : null}
            </div>
          </Panel>
        </div>
      </div>

      {modal === "editar" ? (
        <Modal titulo="Editar ruta" alCerrar={() => setModal(null)}>
          <Formulario campos={CAMPOS_RUTA} inicial={r as never} fuentes={fuentesEdicion} alias={{ recorridoValido: "puntoDestino" }}
            textoEnviar="Guardar cambios" alCancelar={() => setModal(null)}
            alEnviar={async (cuerpo) => {
              await api("PUT", `/api/rutas/${r.id}`, cuerpo);
              avisar("exito", "La ruta se actualizó correctamente.");
              setModal(null);
              await recargar();
            }} />
        </Modal>
      ) : null}
      {modal === "asignar" ? <Asignacion ruta={r} alCerrar={() => setModal(null)} alGuardar={recargar} /> : null}
      {modal === "entrega" ? <RegistroEntrega rutaId={r.id} alCerrar={() => setModal(null)} alGuardar={recargar} /> : null}
    </>
  );
}

const Cifra = ({ titulo, valor, color }: { titulo: string; valor: number; color?: string }) => (
  <div><div className="t">{titulo}</div><div className="v num" style={color ? { color } : undefined}>{miles(valor)}</div></div>
);
const Dato = ({ titulo, valor, ancho }: { titulo: string; valor?: string | null; ancho?: boolean }) => (
  <div className={ancho ? "ancho" : undefined}><dt>{titulo}</dt><dd>{valor ?? "—"}</dd></div>
);
function Recurso({ icono, titulo, principal, secundario }: { icono: NombreIcono; titulo: string; principal: string | null; secundario: string | null }) {
  return (
    <div className="recurso">
      <div className="icono"><Icono nombre={icono} /></div>
      {principal ? <div><strong>{principal}</strong><span>{secundario || titulo}</span></div>
        : <div><strong className="sin">{titulo} sin asignar</strong><span>Requerido para iniciar la ruta</span></div>}
    </div>
  );
}

function Asignacion({ ruta: r, alCerrar, alGuardar }: { ruta: Ruta; alCerrar: () => void; alGuardar: () => Promise<void> }) {
  const avisar = useAvisos();
  const vehiculos = useRecurso<Vehiculo[]>("/api/vehiculos");
  const conductores = useRecurso<Conductor[]>("/api/conductores");
  const enCurso = useRecurso<Ruta[]>("/api/rutas?estado=EN_CURSO");
  const necesidades = useRecurso<Necesidad[]>(`/api/necesidades?operacionId=${r.operacionId}`);
  const operaciones = useRecurso<Operacion[]>("/api/operaciones");
  const [error, setError] = useState<string | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [enviando, setEnviando] = useState(false);

  const cargado = vehiculos.datos && conductores.datos && enCurso.datos && necesidades.datos;
  const frio = operaciones.datos?.find((o) => o.id === r.operacionId)?.requiereCadenaFrio;
  const ocupadoV = Object.fromEntries((enCurso.datos ?? []).filter((x) => x.id !== r.id && x.vehiculoId).map((x) => [x.vehiculoId, x.codigo]));
  const ocupadoC = Object.fromEntries((enCurso.datos ?? []).filter((x) => x.id !== r.id && x.conductorId).map((x) => [x.conductorId, x.codigo]));
  const motivoV = (v: Vehiculo) => !v.activo ? "inactivo" : frio && !v.requiereCadenaFrio ? "sin cadena de frío" : ocupadoV[v.id] ? `en ruta ${ocupadoV[v.id]}` : null;
  const motivoC = (c: Conductor) => !c.activo ? "inactivo" : !licenciaVigente(c) ? "licencia vencida" : ocupadoC[c.id] ? `en ruta ${ocupadoC[c.id]}` : null;

  async function guardar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const datos = new FormData(e.currentTarget);
    const vehiculoId = datos.get("vehiculoId") ? Number(datos.get("vehiculoId")) : null;
    const conductorId = datos.get("conductorId") ? Number(datos.get("conductorId")) : null;
    setEnviando(true);
    setError(null);
    setErrores({});
    try {
      await api("PATCH", `/api/rutas/${r.id}/asignacion`, { vehiculoId, conductorId });
      avisar("exito", `Vehículo y conductor asignados a la ruta ${r.codigo}.`);
      alCerrar();
      await alGuardar();
    } catch (err) {
      setError((err as Error).message);
      if (err instanceof ApiError) setErrores(err.campos);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal titulo={`Asignar recursos · ${r.codigo}`} angosto alCerrar={alCerrar}>
      {!cargado ? <Cargando /> : (
        <form className="formulario" noValidate onSubmit={guardar}>
          {error ? <div className="aviso error" role="alert">{error}</div> : null}
          {necesidades.datos!.length ? (
            <div className="aviso info ancho">
              <div>
                <strong>Necesidad registrada por el cliente</strong>
                {necesidades.datos!.map((n) => (
                  <div key={n.id}>
                    {n.cantidadVehiculos} × {n.tipoVehiculo} (mín. {miles(n.capacidadMinimaKg)} kg{n.requiereCadenaFrio ? ", cadena de frío" : ""}) ·{" "}
                    {n.diasOperacion.map((d) => INICIAL_DIA[d]).join(" ")} · {hora(n.horaInicio)}–{hora(n.horaFin)}
                  </div>
                ))}
              </div>
            </div>
          ) : <div className="aviso warn ancho">El contrato no tiene necesidades registradas. Se recomienda registrarlas antes de asignar recursos.</div>}
          <div className={`campo ancho${errores.vehiculoId ? " con-error" : ""}`}>
            <label htmlFor="f-vehiculoId">Vehículo</label>
            <select id="f-vehiculoId" name="vehiculoId" defaultValue={r.vehiculoId ?? ""}>
              <option value="">Seleccione…</option>
              {vehiculos.datos!.map((v) => {
                const motivo = motivoV(v);
                return <option key={v.id} value={v.id} disabled={Boolean(motivo)}>
                  {v.placa} · {v.tipoVehiculo} {miles(v.capacidadCargaKg)} kg{v.requiereCadenaFrio ? " · frío" : ""}{motivo ? ` — ${motivo}` : ""}
                </option>;
              })}
            </select>
            {frio ? <span className="ayuda">La operación requiere vehículos con cadena de frío.</span> : null}
            {errores.vehiculoId ? <small>{errores.vehiculoId}</small> : null}
          </div>
          <div className={`campo ancho${errores.conductorId ? " con-error" : ""}`}>
            <label htmlFor="f-conductorId">Conductor</label>
            <select id="f-conductorId" name="conductorId" defaultValue={r.conductorId ?? ""}>
              <option value="">Seleccione…</option>
              {conductores.datos!.map((c) => {
                const motivo = motivoC(c);
                return <option key={c.id} value={c.id} disabled={Boolean(motivo)}>{nombreConductor(c)} · {c.numeroLicencia}{motivo ? ` — ${motivo}` : ""}</option>;
              })}
            </select>
            <span className="ayuda">Solo conductores activos con licencia vigente y sin otra ruta en curso.</span>
            {errores.conductorId ? <small>{errores.conductorId}</small> : null}
          </div>
          <div className="acciones-form">
            <button type="button" className="boton secundario" onClick={alCerrar}>Cancelar</button>
            <button type="submit" className="boton primario" disabled={enviando}>{enviando ? "Guardando…" : "Guardar asignación"}</button>
          </div>
        </form>
      )}
    </Modal>
  );
}
