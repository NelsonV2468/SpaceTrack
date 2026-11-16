import type { ReactNode } from "react";
import { DIAS, ESTADOS_RUTA, INICIAL_DIA, porcentaje, type Tono } from "@/lib/formato";
import type { DiaSemana, EstadoRuta } from "@/lib/tipos";

export function Etiqueta({ tono, children }: { tono: Tono; children: ReactNode }) {
  return <span className={`etiqueta ${tono}`}>{children}</span>;
}

export const Activo = ({ valor, femenino }: { valor: boolean; femenino?: boolean }) => (
  <Etiqueta tono={valor ? "ok" : "neutral"}>{valor ? (femenino ? "Activa" : "Activo") : femenino ? "Inactiva" : "Inactivo"}</Etiqueta>
);
export const SiNo = ({ valor }: { valor: boolean }) => <Etiqueta tono={valor ? "info" : "neutral"}>{valor ? "Sí" : "No"}</Etiqueta>;

export function EstadoRutaEtiqueta({ estado }: { estado: EstadoRuta }) {
  const [texto, tono] = ESTADOS_RUTA[estado] ?? [estado, "neutral"];
  return <Etiqueta tono={tono}>{texto}</Etiqueta>;
}

export function Doble({ principal, secundario }: { principal: ReactNode; secundario?: ReactNode }) {
  return (
    <div>
      <strong>{principal}</strong>
      {secundario ? <span className="sec">{secundario}</span> : null}
    </div>
  );
}

export function DiasSemanaChips({ lista }: { lista: DiaSemana[] }) {
  return (
    <span className="dias-semana" title={lista.join(", ")}>
      {DIAS.map((d) => <span key={d} className={lista.includes(d) ? "on" : ""}>{INICIAL_DIA[d]}</span>)}
    </span>
  );
}

export function BarraEntregas({ realizadas, noRealizadas, programadas, texto, alta }: {
  realizadas: number; noRealizadas: number; programadas: number; texto?: string; alta?: boolean;
}) {
  return (
    <div className="progreso">
      <div className="barra" style={alta ? { height: 10 } : undefined}>
        <i className="ok" style={{ width: `${porcentaje(realizadas, programadas)}%` }} />
        <i className="danger" style={{ width: `${porcentaje(noRealizadas, programadas)}%` }} />
      </div>
      <span className="cifra num">{texto ?? `${realizadas} / ${programadas}`}</span>
    </div>
  );
}

export function Leyenda() {
  return (
    <div className="leyenda">
      <span><i style={{ background: "var(--ok)" }} />Entregadas</span>
      <span><i style={{ background: "#d9534a" }} />No entregadas</span>
      <span><i style={{ background: "var(--neutral-soft)" }} />Pendientes</span>
    </div>
  );
}

export function Kpi({ titulo, valor, nota, acento }: { titulo: string; valor: ReactNode; nota: string; acento: string }) {
  return (
    <div className="kpi" style={{ ["--acento" as string]: acento }}>
      <div className="titulo">{titulo}</div>
      <div className="valor num">{valor}</div>
      <div className="nota">{nota}</div>
    </div>
  );
}

export function Vacio({ titulo, texto }: { titulo: string; texto?: ReactNode }) {
  return (
    <div className="vacio">
      <strong>{titulo}</strong>
      {texto}
    </div>
  );
}

export function Cargando() {
  return <div className="vacio" aria-busy="true">Cargando…</div>;
}

export function Encabezado({ titulo, subtitulo, acciones, volver }: {
  titulo: string; subtitulo?: string; acciones?: ReactNode; volver?: ReactNode;
}) {
  return (
    <header className="encabezado">
      <div>
        {volver}
        <h1>{titulo}</h1>
        {subtitulo ? <p className="subtitulo">{subtitulo}</p> : null}
      </div>
      {acciones ? <div className="acciones">{acciones}</div> : null}
    </header>
  );
}

export function Panel({ titulo, extra, children, className }: { titulo?: ReactNode; extra?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`panel ${className ?? ""}`}>
      {titulo !== undefined ? (
        <div className="panel-cabecera">
          <h2>{titulo}</h2>
          {extra}
        </div>
      ) : null}
      {children}
    </section>
  );
}
