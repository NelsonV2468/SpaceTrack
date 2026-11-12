import type { Conductor, DiaSemana, EstadoRuta } from "./tipos";

export const DIAS: DiaSemana[] = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"];
export const INICIAL_DIA: Record<DiaSemana, string> = {
  LUNES: "L", MARTES: "M", MIERCOLES: "X", JUEVES: "J", VIERNES: "V", SABADO: "S", DOMINGO: "D",
};
export const NOMBRE_DIA: Record<DiaSemana, string> = {
  LUNES: "Lunes", MARTES: "Martes", MIERCOLES: "Miércoles", JUEVES: "Jueves", VIERNES: "Viernes", SABADO: "Sábado", DOMINGO: "Domingo",
};
export const TIPOS_VEHICULO = ["Camion", "Camioneta", "Furgon", "Motocicleta"];
export const REFRESCO_MAPA_MS = 5000;

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

export function fecha(valor?: string | null): string {
  if (!valor) return "—";
  const [a, m, d] = valor.slice(0, 10).split("-");
  return `${d} ${MESES[Number(m) - 1]} ${a}`;
}
export const fechaHora = (valor?: string | null) => (valor ? `${fecha(valor)}, ${valor.slice(11, 16)}` : "—");
export const hora = (valor?: string | null) => (valor ? valor.slice(0, 5) : "");
export const miles = (valor?: number | null) => Number(valor ?? 0).toLocaleString("es-CO");
export const porcentaje = (parte: number, total: number) => (total ? Math.round((parte * 100) / total) : 0);

export function hoy(dias = 0): string {
  const f = new Date();
  f.setDate(f.getDate() + dias);
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, "0")}-${String(f.getDate()).padStart(2, "0")}`;
}

export const nombreConductor = (c: Pick<Conductor, "nombres" | "apellidos">) => `${c.nombres} ${c.apellidos}`;
export const licenciaVigente = (c: Pick<Conductor, "fechaVencimientoLicencia">) => c.fechaVencimientoLicencia >= hoy(0);

export const ESTADOS_RUTA: Record<EstadoRuta, [string, Tono]> = {
  PENDIENTE: ["Pendiente", "neutral"],
  EN_CURSO: ["En curso", "info"],
  FINALIZADA: ["Finalizada", "ok"],
};
export const TONO_OPERACION: Record<string, Tono> = { ACTIVA: "ok", PAUSADA: "warn", FINALIZADA: "neutral" };

export type Tono = "ok" | "info" | "warn" | "danger" | "neutral";

export const capitalizar = (texto: string) => texto.charAt(0) + texto.slice(1).toLowerCase();
