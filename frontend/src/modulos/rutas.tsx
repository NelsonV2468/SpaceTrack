"use client";

// Formulario de rutas. Al elegir la operación (contrato) y la zona se completan solos el código, el nombre,
// el destino y las entregas programadas; el usuario solo revisa y ajusta si quiere.
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Cargando } from "@/components/ui";
import { ApiError, apiGet } from "@/lib/api";
import { fecha, miles } from "@/lib/formato";
import type { Operacion, Ruta, Zona } from "@/lib/tipos";

type Campo = "operacionId" | "zonaId" | "codigo" | "nombre" | "puntoOrigen" | "puntoDestino" | "numeroEntregasProgramadas";
type Valores = Record<Campo, string>;

export const ORIGEN_POR_DEFECTO = "CEDI Puente Aranda";
// Errores de validacion de clase que se muestran junto a un campo concreto.
const ALIAS: Record<string, Campo> = { recorridoValido: "puntoDestino" };

export interface PropsFormularioRuta {
  inicial?: Ruta | null;
  /** Operacion preseleccionada (p. ej. desde «Crear ruta» en Operaciones). */
  operacionInicial?: number;
  textoEnviar: string;
  alEnviar: (cuerpo: Record<string, unknown>) => Promise<void>;
  alCancelar: () => void;
}

export function FormularioRuta({ inicial, operacionInicial, textoEnviar, alEnviar, alCancelar }: PropsFormularioRuta) {
  const [operaciones, setOperaciones] = useState<Operacion[]>();
  const [zonas, setZonas] = useState<Zona[]>([]);
  const [rutas, setRutas] = useState<Ruta[]>([]);
  const [valores, setValores] = useState<Valores>(() => ({
    operacionId: inicial ? String(inicial.operacionId) : operacionInicial ? String(operacionInicial) : "",
    zonaId: inicial ? String(inicial.zonaId) : "",
    codigo: inicial?.codigo ?? "",
    nombre: inicial?.nombre ?? "",
    puntoOrigen: inicial?.puntoOrigen ?? ORIGEN_POR_DEFECTO,
    puntoDestino: inicial?.puntoDestino ?? "",
    numeroEntregasProgramadas: inicial ? String(inicial.numeroEntregasProgramadas) : "",
  }));
  // Campos que el usuario escribio a mano: el autollenado ya no los toca. Al editar, se respetan todos.
  const [tocados, setTocados] = useState<Set<Campo>>(() => new Set(inicial ? (Object.keys(valores) as Campo[]) : []));
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let vigente = true;
    Promise.all([
      apiGet<Operacion[]>("/api/operaciones").catch(() => []),
      apiGet<Zona[]>("/api/zonas").catch(() => []),
      apiGet<Ruta[]>("/api/rutas").catch(() => []),
    ]).then(([o, z, r]) => {
      if (!vigente) return;
      setOperaciones(o); setZonas(z); setRutas(r);
    });
    return () => { vigente = false; };
  }, []);

  const operacion = operaciones?.find((o) => String(o.id) === valores.operacionId);
  const zona = zonas.find((z) => String(z.id) === valores.zonaId);
  const opcionesOperacion = (operaciones ?? []).filter((o) => o.estado === "ACTIVA" || String(o.id) === valores.operacionId);
  // Solo las localidades de Bogota guardadas en la base; una zona antigua solo aparece si es la de esta ruta.
  const opcionesZona = zonas.filter((z) => (z.catalogo && z.activa) || String(z.id) === valores.zonaId);

  const cupo = useMemo(() => {
    if (!operacion) return null;
    const enOtras = rutas.filter((r) => r.operacionId === operacion.id && r.id !== inicial?.id)
      .reduce((s, r) => s + r.numeroEntregasProgramadas, 0);
    const disponibles = Math.max(0, operacion.numeroEntregasEsperadas - enOtras);
    const porVehiculo = Math.ceil(operacion.numeroEntregasEsperadas / Math.max(1, operacion.numeroVehiculosRequeridos));
    return { enOtras, disponibles, sugerido: Math.max(1, Math.min(disponibles, porVehiculo)) };
  }, [operacion, rutas, inicial?.id]);

  // Autollenado: se recalcula al cambiar la operacion o la zona, sin pisar lo que el usuario escribio.
  useEffect(() => {
    if (!operaciones) return;
    const sugerencias: Partial<Valores> = {};
    if (operacion && cupo) {
      sugerencias.numeroEntregasProgramadas = String(cupo.sugerido);
      sugerencias.codigo = codigoSugerido(operacion, rutas);
    }
    if (zona) sugerencias.puntoDestino = `Centro de ${zona.localidad}, Bogotá`;
    if (operacion && zona) sugerencias.nombre = `${operacion.nombreOperacion} – ${zona.localidad}`.slice(0, 120);
    else if (zona) sugerencias.nombre = `Distribución en ${zona.localidad}`;
    setValores((v) => {
      const nuevo = { ...v };
      for (const [campo, valor] of Object.entries(sugerencias) as [Campo, string][]) if (!tocados.has(campo)) nuevo[campo] = valor;
      return nuevo;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [operaciones, valores.operacionId, valores.zonaId]);

  function cambiar(campo: Campo, valor: string) {
    setValores((v) => ({ ...v, [campo]: valor }));
    // Elegir operacion o zona dispara el autollenado; escribir en un campo lo marca como propio.
    if (campo !== "operacionId" && campo !== "zonaId") setTocados((t) => new Set(t).add(campo));
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setEnviando(true);
    setErrores({});
    setGeneral(null);
    const numero = (v: string) => (v.trim() === "" ? null : Number(v));
    try {
      await alEnviar({
        codigo: valores.codigo.trim(), nombre: valores.nombre.trim(),
        zonaId: numero(valores.zonaId), operacionId: numero(valores.operacionId),
        puntoOrigen: valores.puntoOrigen.trim(), puntoDestino: valores.puntoDestino.trim(),
        numeroEntregasProgramadas: numero(valores.numeroEntregasProgramadas),
      });
    } catch (error) {
      const porCampo: Record<string, string> = {};
      const sueltos: string[] = [];
      if (error instanceof ApiError) {
        for (const [campo, texto] of Object.entries(error.campos)) {
          const destino = ALIAS[campo] ?? campo;
          if (destino in valores) porCampo[destino] = texto; else sueltos.push(texto);
        }
      }
      setErrores(porCampo);
      setGeneral((error instanceof Error ? error.message : "Error") + (sueltos.length ? `: ${sueltos.join(" ")}` : ""));
    } finally {
      setEnviando(false);
    }
  }

  if (!operaciones) return <Cargando />;

  const texto = (campo: Campo, etiqueta: string, opciones: { tipo?: string; ayuda?: string; ancho?: boolean } = {}) => (
    <div className={`campo${opciones.ancho ? " ancho" : ""}${errores[campo] ? " con-error" : ""}`}>
      <label htmlFor={`f-${campo}`}>{etiqueta}</label>
      <input id={`f-${campo}`} name={campo} type={opciones.tipo ?? "text"} value={valores[campo]}
        step={opciones.tipo === "number" ? "any" : undefined} inputMode={opciones.tipo === "number" ? "decimal" : undefined}
        onChange={(e) => cambiar(campo, e.target.value)} />
      {opciones.ayuda ? <span className="ayuda">{opciones.ayuda}</span> : null}
      {errores[campo] ? <small role="alert">{errores[campo]}</small> : null}
    </div>
  );
  const auto = (campo: Campo) => (!inicial && !tocados.has(campo) && valores[campo] ? "Completado automáticamente; puede ajustarlo." : undefined);

  return (
    <form className="formulario" noValidate onSubmit={enviar}>
      {general ? <div className="aviso error" role="alert">{general}</div> : null}
      {!inicial ? (
        <div className="aviso info">Elija la operación y la zona: el resto de los datos se completa solo con la información del contrato.</div>
      ) : null}

      <div className={`campo ancho${errores.operacionId ? " con-error" : ""}`}>
        <label htmlFor="f-operacionId">1. Operación / contrato</label>
        <select id="f-operacionId" name="operacionId" value={valores.operacionId} onChange={(e) => cambiar("operacionId", e.target.value)}>
          <option value="">Seleccione…</option>
          {opcionesOperacion.map((o) => <option key={o.id} value={o.id}>{o.codigoContrato} — {o.razonSocialCliente} · {o.nombreOperacion}</option>)}
        </select>
        {!opcionesOperacion.length ? <span className="ayuda">No hay operaciones activas. Registre una en Operaciones.</span> : null}
        {errores.operacionId ? <small role="alert">{errores.operacionId}</small> : null}
      </div>

      {operacion && cupo ? (
        <div className="aviso neutro resumen-operacion">
          <dl className="ficha">
            <div><dt>Cliente</dt><dd>{operacion.razonSocialCliente}</dd></div>
            <div><dt>Vigencia</dt><dd>{fecha(operacion.fechaInicio)} – {fecha(operacion.fechaFin)}</dd></div>
            <div><dt>Días de operación</dt><dd>{operacion.diasOperacion}</dd></div>
            <div><dt>Cadena de frío</dt><dd>{operacion.requiereCadenaFrio ? "Sí, requiere vehículo refrigerado" : "No"}</dd></div>
            <div className="ancho"><dt>Entregas del contrato</dt>
              <dd>{miles(operacion.numeroEntregasEsperadas)} esperadas · {miles(cupo.enOtras)} ya programadas en otras rutas · <strong>{miles(cupo.disponibles)} disponibles</strong></dd></div>
          </dl>
          {cupo.disponibles === 0 ? <div className="aviso warn">Este contrato ya tiene programadas todas sus entregas esperadas.</div> : null}
        </div>
      ) : null}

      <div className={`campo ancho${errores.zonaId ? " con-error" : ""}`}>
        <label htmlFor="f-zonaId">2. Zona (localidad de Bogotá)</label>
        <select id="f-zonaId" name="zonaId" value={valores.zonaId} onChange={(e) => cambiar("zonaId", e.target.value)}>
          <option value="">Seleccione…</option>
          {opcionesZona.map((z) => <option key={z.id} value={z.id}>{z.catalogo ? z.localidad : `${z.nombre} (${z.localidad})`}</option>)}
        </select>
        {errores.zonaId ? <small role="alert">{errores.zonaId}</small> : null}
      </div>

      {texto("codigo", "Código de la ruta", { ayuda: auto("codigo") ?? "Ej.: R-001" })}
      {texto("numeroEntregasProgramadas", "Entregas programadas", { tipo: "number",
        ayuda: cupo ? `Máximo disponible en el contrato: ${miles(cupo.disponibles)}` : auto("numeroEntregasProgramadas") })}
      {texto("nombre", "Nombre de la ruta", { ancho: true, ayuda: auto("nombre") })}
      {texto("puntoOrigen", "Punto de origen")}
      {texto("puntoDestino", "Punto de destino", { ayuda: auto("puntoDestino") })}

      <div className="acciones-form">
        <button type="button" className="boton secundario" onClick={alCancelar}>Cancelar</button>
        <button type="submit" className="boton primario" disabled={enviando}>{enviando ? "Guardando…" : textoEnviar}</button>
      </div>
    </form>
  );
}

/** Siguiente codigo libre para la operacion, por ejemplo R-CT-DEMO-001-06. */
function codigoSugerido(operacion: Operacion, rutas: Ruta[]): string {
  const usados = new Set(rutas.map((r) => r.codigo.toUpperCase()));
  const base = `R-${operacion.codigoContrato.toUpperCase()}`.slice(0, 26);
  let n = rutas.filter((r) => r.operacionId === operacion.id).length + 1;
  while (usados.has(`${base}-${String(n).padStart(2, "0")}`)) n++;
  return `${base}-${String(n).padStart(2, "0")}`;
}
