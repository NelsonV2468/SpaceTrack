"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAvisos } from "@/components/Avisos";
import { Formulario } from "@/components/Formulario";
import { Icono } from "@/components/Icono";
import { Modal } from "@/components/Modal";
import { Cargando, Encabezado, Vacio } from "@/components/ui";
import { api, apiGet } from "@/lib/api";
import { useRecurso } from "@/lib/hooks";
import { useSesion } from "@/lib/sesion";
import { MODULOS } from "./config";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Registro = Record<string, any> & { id: number };

export function ModuloCrud({ clave }: { clave: string }) {
  const m = MODULOS[clave];
  const router = useRouter();
  const avisar = useAvisos();
  const { puedeEscribir, esAdmin } = useSesion();
  const [filtro, setFiltro] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [edicion, setEdicion] = useState<{ item: Registro | null } | null>(null);
  const [fuentes, setFuentes] = useState<Record<string, Registro[]>>({});

  const url = m.api + (m.filtro && filtro ? `?${m.filtro.parametro}=${encodeURIComponent(filtro)}` : "");
  const { datos, error, cargando, recargar } = useRecurso<Registro[]>(url);

  useEffect(() => { setBusqueda(""); setFiltro(""); }, [clave]);

  const lista = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    const todos = datos ?? [];
    return q ? todos.filter((item) => Object.values(item).join(" ").toLowerCase().includes(q)) : todos;
  }, [datos, busqueda]);

  async function abrirFormulario(item: Registro | null) {
    const necesarias = [...new Set(m.campos.filter((c) => c.tipo === "relacion").map((c) => c.fuente!))];
    const cargadas = await Promise.all(necesarias.map((f) => apiGet<Registro[]>(MODULOS[f].api).catch(() => [])));
    setFuentes(Object.fromEntries(necesarias.map((f, i) => [f, cargadas[i]])));
    setEdicion({ item });
  }

  async function guardar(cuerpo: Record<string, unknown>) {
    const item = edicion?.item;
    const guardado = await api<Registro>(item ? "PUT" : "POST", m.api + (item ? `/${item.id}` : ""), cuerpo);
    avisar("exito", `${m.femenino ? "La" : "El"} ${m.singular} se ${item ? "actualizó" : "registró"} correctamente.`);
    setEdicion(null);
    if (!item && m.enlaceFila && guardado?.id) router.push(m.enlaceFila(guardado));
    else await recargar();
  }

  async function eliminar(item: Registro) {
    if (!confirm(`¿Desea eliminar ${m.femenino ? "esta" : "este"} ${m.singular}? Esta acción no se puede deshacer.`)) return;
    try {
      await api("DELETE", `${m.api}/${item.id}`);
      avisar("exito", "Registro eliminado correctamente.");
    } catch (e) {
      avisar("error", e instanceof Error ? e.message : "No se pudo eliminar");
    }
    await recargar();
  }

  const conAcciones = !m.soloDetalle && (puedeEscribir || esAdmin);

  return (
    <>
      <Encabezado titulo={m.titulo} subtitulo={m.descripcion}
        acciones={puedeEscribir ? (
          <button className="boton primario" onClick={() => abrirFormulario(null)}><Icono nombre="mas" />Registrar {m.singular}</button>
        ) : null} />

      <section className="panel">
        <div className="herramientas">
          <div className="izq">
            <label className="buscador">
              <Icono nombre="buscar" />
              <input type="search" placeholder={`Buscar ${(m.menu ?? m.titulo).toLowerCase()}…`} aria-label="Buscar"
                value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            </label>
            {m.filtro ? (
              <select aria-label={m.filtro.etiqueta} className="selector-filtro" value={filtro} onChange={(e) => setFiltro(e.target.value)}>
                <option value="">Todos los estados</option>
                {m.filtro.opciones.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
              </select>
            ) : null}
          </div>
          <span className="total">{lista.length} {lista.length === 1 ? "registro" : "registros"}</span>
        </div>

        {cargando && !datos ? <Cargando /> : error && !datos ? (
          <Vacio titulo="No se pudo cargar la información" texto={error} />
        ) : !lista.length ? (
          <Vacio titulo={busqueda ? "Sin coincidencias" : filtro ? "Sin registros en este estado" : "Aún no hay registros"}
            texto={busqueda ? `No hay registros que coincidan con «${busqueda}».` : puedeEscribir ? `Use «Registrar ${m.singular}» para crear el primero.` : undefined} />
        ) : (
          <div className="tabla">
            <table>
              <thead>
                <tr>
                  {m.columnas.map(([t]) => <th key={t}>{t}</th>)}
                  {conAcciones || m.enlaceFila ? <th className="derecha"><span className="solo-lector">Acciones</span></th> : null}
                </tr>
              </thead>
              <tbody>
                {lista.map((item) => (
                  <tr key={item.id} className={m.enlaceFila ? "clic" : undefined}
                    onClick={m.enlaceFila ? () => router.push(m.enlaceFila!(item)) : undefined}>
                    {m.columnas.map(([t, render]) => <td key={t} data-titulo={t}>{render(item)}</td>)}
                    {m.enlaceFila ? (
                      <td className="derecha"><Link className="boton secundario chico" href={m.enlaceFila(item)} onClick={(e) => e.stopPropagation()}>Ver detalle</Link></td>
                    ) : conAcciones ? (
                      <td className="derecha">
                        <div className="acciones-fila">
                          {puedeEscribir ? <button className="boton secundario chico" onClick={() => abrirFormulario(item)}>Editar</button> : null}
                          {esAdmin ? <button className="boton peligro chico" onClick={() => eliminar(item)}>Eliminar</button> : null}
                        </div>
                      </td>
                    ) : null}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {edicion ? (
        <Modal titulo={`${edicion.item ? "Editar" : m.femenino ? "Nueva" : "Nuevo"} ${m.singular}`} alCerrar={() => setEdicion(null)}>
          <Formulario campos={m.campos} inicial={edicion.item} fuentes={fuentes} alias={m.alias}
            textoEnviar={edicion.item ? "Guardar cambios" : `Registrar ${m.singular}`}
            alEnviar={guardar} alCancelar={() => setEdicion(null)} />
        </Modal>
      ) : null}
    </>
  );
}
