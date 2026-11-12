"use client";

import Link from "next/link";
import { useAvisos } from "@/components/Avisos";
import { Formulario, type CampoDef } from "@/components/Formulario";
import { Modal } from "@/components/Modal";
import { Doble, Etiqueta, Vacio } from "@/components/ui";
import { api } from "@/lib/api";
import { fechaHora } from "@/lib/formato";
import { useSesion } from "@/lib/sesion";
import type { Entrega, Ruta } from "@/lib/tipos";

const CAMPOS_ENTREGA: CampoDef[] = [
  { nombre: "numeroGuia", etiqueta: "Número de guía", opcional: true },
  { nombre: "destinatario", etiqueta: "Destinatario" },
  { nombre: "direccion", etiqueta: "Dirección de entrega", ancho: true },
  { nombre: "estado", etiqueta: "Resultado", tipo: "select", opciones: [["ENTREGADA", "Entregada"], ["NO_ENTREGADA", "No entregada (con novedad)"]] },
  { nombre: "novedad", etiqueta: "Novedad", tipo: "textarea", opcional: true, ancho: true,
    ayuda: "Obligatoria si la entrega no se realizó (p. ej. cliente ausente, dirección errada)." },
];

/** Registro de una entrega. Sin `rutaId` permite elegir entre las rutas en curso con entregas pendientes. */
export function RegistroEntrega({ rutaId, rutasEnCurso = [], alCerrar, alGuardar }: {
  rutaId?: number; rutasEnCurso?: Ruta[]; alCerrar: () => void; alGuardar: () => Promise<void> | void;
}) {
  const avisar = useAvisos();
  const disponibles = rutasEnCurso.filter((r) => r.entregasRealizadas + r.entregasNoRealizadas < r.numeroEntregasProgramadas);
  const campos: CampoDef[] = rutaId ? CAMPOS_ENTREGA : [
    { nombre: "rutaId", etiqueta: "Ruta en curso", tipo: "relacion", fuente: "rutas", ancho: true,
      mostrar: (r) => `${r.codigo} · ${r.nombre} (${Number(r.entregasRealizadas) + Number(r.entregasNoRealizadas)}/${r.numeroEntregasProgramadas})` },
    ...CAMPOS_ENTREGA,
  ];

  return (
    <Modal titulo="Registrar entrega" alCerrar={alCerrar}>
      <Formulario campos={campos} fuentes={{ rutas: disponibles as never }} textoEnviar="Registrar entrega" alias={{ novedadValida: "novedad" }}
        alCancelar={alCerrar}
        alEnviar={async (cuerpo) => {
          await api("POST", "/api/entregas", { ...cuerpo, rutaId: rutaId ?? cuerpo.rutaId });
          avisar("exito", "Entrega registrada correctamente.");
          alCerrar();
          await alGuardar();
        }} />
    </Modal>
  );
}

export function TablaEntregas({ entregas, puedeAnular, mostrarRuta, alCambiar }: {
  entregas: Entrega[]; puedeAnular: (e: Entrega) => boolean; mostrarRuta?: boolean; alCambiar: () => Promise<void> | void;
}) {
  const avisar = useAvisos();
  const { esAdmin } = useSesion();

  async function anular(e: Entrega) {
    if (!confirm("¿Desea anular esta entrega? Esta acción no se puede deshacer.")) return;
    try {
      await api("DELETE", `/api/entregas/${e.id}`);
      avisar("exito", "Entrega anulada.");
    } catch (error) {
      avisar("error", (error as Error).message);
    }
    await alCambiar();
  }

  if (!entregas.length) return <Vacio titulo="Sin entregas registradas" texto="Las entregas aparecen aquí a medida que se registran." />;
  return (
    <div className="tabla">
      <table>
        <thead><tr><th>{mostrarRuta ? "Ruta" : "#"}</th><th>Destinatario</th><th>Guía</th><th>Resultado</th><th>Registro</th><th className="derecha"><span className="solo-lector">Acciones</span></th></tr></thead>
        <tbody>
          {entregas.map((e, i) => (
            <tr key={e.id}>
              <td className="num" data-titulo={mostrarRuta ? "Ruta" : "#"}>
                {mostrarRuta ? <Link className="enlace" href={`/panel/rutas/detalle/?id=${e.rutaId}`}>{e.codigoRuta}</Link> : i + 1}
              </td>
              <td data-titulo="Destinatario"><Doble principal={e.destinatario} secundario={e.direccion} /></td>
              <td className="num" data-titulo="Guía">{e.numeroGuia || "—"}</td>
              <td data-titulo="Resultado">
                <div>
                  {e.estado === "ENTREGADA" ? <Etiqueta tono="ok">Entregada</Etiqueta> : <Etiqueta tono="danger">No entregada</Etiqueta>}
                  {e.novedad ? <span className="sec">{e.novedad}</span> : null}
                </div>
              </td>
              <td className="num" data-titulo="Registro">{fechaHora(e.fechaRegistro)}</td>
              <td className="derecha">
                {esAdmin && puedeAnular(e) ? <button className="boton peligro chico" onClick={() => anular(e)}>Anular</button> : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
