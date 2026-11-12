"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { MapaBogota } from "@/components/MapaBogota";
import { Encabezado, EstadoRutaEtiqueta, Panel, Vacio } from "@/components/ui";
import { REFRESCO_MAPA_MS } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import { colorRuta, mensajeSinUbicacion, tieneUbicacion } from "@/lib/mapa";
import type { Ubicacion } from "@/lib/tipos";

type Filtro = "EN_CURSO" | "ACTIVAS" | "TODAS";

export default function Monitoreo() {
  const [filtro, setFiltro] = useState<Filtro>("ACTIVAS");
  const [seleccion, setSeleccion] = useState<number | null>(null);
  const { datos, error } = useRecurso<Ubicacion[]>("/api/rutas/ubicaciones", REFRESCO_MAPA_MS);
  const [actualizado, setActualizado] = useState("");

  useEffect(() => { if (datos) setActualizado(new Date().toLocaleTimeString("es-CO")); }, [datos]);
  const ubicaciones = useMemo(() => datos ?? [], [datos]);
  const colores = useMemo(() => Object.fromEntries(ubicaciones.map((u, i) => [u.rutaId, colorRuta(i)])), [ubicaciones]);
  const visibles = ubicaciones.filter((u) => filtro === "TODAS" || u.estado === "EN_CURSO" || (filtro === "ACTIVAS" && u.estado === "PENDIENTE"));
  const seleccionVigente = seleccion !== null && visibles.some((u) => u.rutaId === seleccion && tieneUbicacion(u)) ? seleccion : null;
  const opciones = useMemo(() => ({ seleccion: seleccionVigente, colores }), [seleccionVigente, colores]);
  const alternar = (id: number) => setSeleccion((s) => (s === id ? null : id));

  return (
    <>
      <Encabezado titulo="Monitoreo de flota" subtitulo={`Ubicación simulada de los vehículos. Se actualiza cada ${REFRESCO_MAPA_MS / 1000} segundos.`}
        acciones={
          <select aria-label="Rutas a mostrar" value={filtro} onChange={(e) => { setFiltro(e.target.value as Filtro); setSeleccion(null); }}>
            <option value="EN_CURSO">Solo rutas en curso</option>
            <option value="ACTIVAS">Pendientes y en curso</option>
            <option value="TODAS">Todas las rutas</option>
          </select>
        } />

      <div className="monitoreo">
        <Panel titulo="Plano de Bogotá D.C." extra={<span className="total num">{error ?? (actualizado ? `Actualizado ${actualizado}` : "")}</span>}>
          <MapaBogota ubicaciones={visibles} opciones={opciones} alSeleccionar={alternar} />
          <div className="mapa-pie">
            <div className="leyenda">
              <span><i style={{ background: "#102741" }} />Centro de distribución</span>
              <span><i style={{ background: "#0f7b4f", borderRadius: "50%" }} />Entregada</span>
              <span><i style={{ background: "#c0392b", borderRadius: "50%" }} />No entregada</span>
              <span><i style={{ background: "#fff", border: "1.5px solid #6b778c", borderRadius: "50%" }} />Pendiente</span>
            </div>
          </div>
        </Panel>

        <Panel titulo="Unidades" extra={<span className="total">{visibles.length} {visibles.length === 1 ? "ruta" : "rutas"}</span>}>
          <div className="unidades">
            {visibles.length ? visibles.map((u) => {
              const sinUbicacion = mensajeSinUbicacion(u);
              return (
                <div key={u.rutaId} className={`unidad${sinUbicacion ? " sin-ubicacion" : ""}${seleccionVigente === u.rutaId ? " sel" : ""}`}
                  onClick={sinUbicacion ? undefined : () => alternar(u.rutaId)}>
                  <div className="fila">
                    <strong>{sinUbicacion ? null : <span className="color" style={{ background: colores[u.rutaId], marginRight: 8 }} />}{u.placaVehiculo || "Sin vehículo"}</strong>
                    <EstadoRutaEtiqueta estado={u.estado} />
                  </div>
                  <div className="desc">{u.codigo} · {u.nombre}{u.nombreConductor ? ` · ${u.nombreConductor}` : ""}</div>
                  <div className="desc">{sinUbicacion || `${u.descripcionPosicion}${u.velocidadKmh ? ` · ${u.velocidadKmh} km/h` : ""}`}</div>
                  <div className="fila">
                    <div className="progreso" style={{ flex: 1 }}>
                      <div className="barra"><i className="info" style={{ width: `${Math.round(u.progreso * 100)}%` }} /></div>
                      <span className="cifra num">{u.entregasRegistradas}/{u.entregasProgramadas}</span>
                    </div>
                    <Link className="enlace" href={`/panel/rutas/detalle/?id=${u.rutaId}`} onClick={(e) => e.stopPropagation()}>Detalle</Link>
                  </div>
                </div>
              );
            }) : <Vacio titulo="Sin rutas para mostrar" texto={filtro === "EN_CURSO" ? "Inicie una ruta para verla en el mapa." : "Registre rutas para verlas aquí."} />}
          </div>
        </Panel>
      </div>
    </>
  );
}
