"use client";

import { useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/lib/api";
import { DIAS, NOMBRE_DIA, hora } from "@/lib/formato";

type Registro = Record<string, unknown> & { id?: number };

export interface CampoDef {
  nombre: string;
  etiqueta: string;
  tipo?: "text" | "email" | "number" | "date" | "time" | "check" | "select" | "relacion" | "dias" | "textarea";
  /** Para "select": valores, o pares [valor, texto]. */
  opciones?: (string | [string, string])[];
  /** Para "relacion": lista de registros disponibles y como mostrarlos. */
  fuente?: string;
  mostrar?: (registro: Registro) => string;
  filtrar?: (registro: Registro) => boolean;
  valor?: unknown;
  ancho?: boolean;
  opcional?: boolean;
  ayuda?: string;
}

interface Props {
  campos: CampoDef[];
  inicial?: Registro | null;
  fuentes?: Record<string, Registro[] | undefined>;
  textoEnviar: string;
  /** Validaciones de clase (p. ej. "periodoValido") que se muestran junto a un campo concreto. */
  alias?: Record<string, string>;
  antes?: ReactNode;
  alEnviar: (cuerpo: Record<string, unknown>) => Promise<void>;
  alCancelar: () => void;
}

function valorInicial(campo: CampoDef, item?: Registro | null): unknown {
  if (!item) return campo.valor;
  const valor = item[campo.nombre];
  return campo.tipo === "time" ? hora(valor as string) : valor;
}

function leer(form: HTMLFormElement, campos: CampoDef[]): Record<string, unknown> {
  const datos = new FormData(form);
  const cuerpo: Record<string, unknown> = {};
  for (const campo of campos) {
    if (campo.tipo === "check") cuerpo[campo.nombre] = datos.get(campo.nombre) === "on";
    else if (campo.tipo === "dias") cuerpo[campo.nombre] = datos.getAll(campo.nombre);
    else {
      const valor = String(datos.get(campo.nombre) ?? "").trim();
      const numerico = campo.tipo === "number" || campo.tipo === "relacion";
      cuerpo[campo.nombre] = valor === "" ? (campo.opcional || numerico ? null : "") : numerico ? Number(valor) : valor;
    }
  }
  return cuerpo;
}

export function Formulario({ campos, inicial, fuentes = {}, textoEnviar, alias = {}, antes, alEnviar, alCancelar }: Props) {
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setEnviando(true);
    setErrores({});
    setGeneral(null);
    try {
      await alEnviar(leer(e.currentTarget, campos));
    } catch (error) {
      const campoErrores: Record<string, string> = {};
      const sueltos: string[] = [];
      const nombres = new Set(campos.map((c) => c.nombre));
      if (error instanceof ApiError) {
        for (const [campo, texto] of Object.entries(error.campos)) {
          const destino = alias[campo] ?? campo;
          if (nombres.has(destino)) campoErrores[destino] = texto;
          else sueltos.push(texto);
        }
      }
      setErrores(campoErrores);
      setGeneral((error instanceof Error ? error.message : "Error") + (sueltos.length ? `: ${sueltos.join(" ")}` : ""));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="formulario" noValidate onSubmit={enviar}>
      {general ? <div className="aviso error" role="alert">{general}</div> : null}
      {antes}
      {campos.map((campo) => (
        <Campo key={campo.nombre} campo={campo} valor={valorInicial(campo, inicial)} item={inicial}
          opcionesRelacion={campo.fuente ? fuentes[campo.fuente] : undefined} error={errores[campo.nombre]} />
      ))}
      <div className="acciones-form">
        <button type="button" className="boton secundario" onClick={alCancelar}>Cancelar</button>
        <button type="submit" className="boton primario" disabled={enviando}>{enviando ? "Guardando…" : textoEnviar}</button>
      </div>
    </form>
  );
}

function Campo({ campo, valor, item, opcionesRelacion, error }: {
  campo: CampoDef; valor: unknown; item?: Registro | null; opcionesRelacion?: Registro[]; error?: string;
}) {
  const id = `f-${campo.nombre}`;
  const clase = `campo${campo.ancho ? " ancho" : ""}${error ? " con-error" : ""}`;

  if (campo.tipo === "check") {
    return (
      <div className={clase}>
        <label className="check"><input type="checkbox" name={campo.nombre} defaultChecked={Boolean(valor)} />{campo.etiqueta}</label>
      </div>
    );
  }

  let control: ReactNode;
  let ayudaVacia: string | null = null;
  if (campo.tipo === "dias") {
    const seleccion = (item?.diasOperacion as string[] | undefined) ?? DIAS.filter((d) => d !== "DOMINGO");
    control = (
      <div className="selector-dias" role="group" aria-label={campo.etiqueta}>
        {DIAS.map((dia) => (
          <label key={dia}><input type="checkbox" name={campo.nombre} value={dia} defaultChecked={seleccion.includes(dia)} />{NOMBRE_DIA[dia]}</label>
        ))}
      </div>
    );
  } else if (campo.tipo === "select") {
    const opciones = (campo.opciones ?? []).map((o) => (Array.isArray(o) ? o : [o, o]));
    control = (
      <select id={id} name={campo.nombre} defaultValue={String(valor ?? opciones[0]?.[0] ?? "")}>
        {opciones.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
      </select>
    );
  } else if (campo.tipo === "relacion") {
    const lista = (opcionesRelacion ?? []).filter((o) => !campo.filtrar || campo.filtrar(o) || (item && o.id === item[campo.nombre]));
    if (!lista.length) ayudaVacia = "No hay registros disponibles. Regístrelos primero.";
    control = (
      <select id={id} name={campo.nombre} defaultValue={valor == null ? "" : String(valor)}>
        <option value="">Seleccione…</option>
        {lista.map((o) => <option key={o.id} value={o.id}>{campo.mostrar?.(o)}</option>)}
      </select>
    );
  } else if (campo.tipo === "textarea") {
    control = <textarea id={id} name={campo.nombre} defaultValue={String(valor ?? "")} />;
  } else {
    control = (
      <input id={id} name={campo.nombre} type={campo.tipo ?? "text"} defaultValue={valor == null ? "" : String(valor)}
        step={campo.tipo === "number" ? "any" : undefined} inputMode={campo.tipo === "number" ? "decimal" : undefined} />
    );
  }

  return (
    <div className={clase}>
      <label htmlFor={campo.tipo === "dias" ? undefined : id}>{campo.etiqueta}{campo.opcional ? " (opcional)" : ""}</label>
      {control}
      {ayudaVacia ? <span className="ayuda">{ayudaVacia}</span> : null}
      {campo.ayuda ? <span className="ayuda">{campo.ayuda}</span> : null}
      {error ? <small role="alert">{error}</small> : null}
    </div>
  );
}
