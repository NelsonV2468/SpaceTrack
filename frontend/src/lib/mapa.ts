// Plano esquematico de Bogota en SVG. Funciona sin internet: no usa mosaicos externos.
import type { PuntoMapa, Ubicacion } from "./tipos";

const LIMITES = { latMin: 4.48, latMax: 4.78, lngMin: -74.25, lngMax: -74.0 };
const ANCHO = 1000;
const ALTO = Math.round((ANCHO * (LIMITES.latMax - LIMITES.latMin)) / (LIMITES.lngMax - LIMITES.lngMin));
const COLORES = ["#1d4ed8", "#b45309", "#047857", "#7c3aed", "#be123c", "#0e7490", "#4d7c0f", "#a21caf"];

type Par = [number, number];
const AREA_URBANA: Par[] = [[4.775, -74.035], [4.775, -74.095], [4.745, -74.125], [4.72, -74.15], [4.69, -74.165],
  [4.66, -74.19], [4.64, -74.205], [4.61, -74.215], [4.59, -74.205], [4.56, -74.18], [4.535, -74.165],
  [4.5, -74.14], [4.485, -74.12], [4.5, -74.095], [4.54, -74.085], [4.56, -74.07], [4.59, -74.06],
  [4.62, -74.055], [4.66, -74.045], [4.7, -74.025], [4.74, -74.02]];
const CERROS: Par[] = [[4.78, -74.018], [4.7, -74.02], [4.66, -74.042], [4.62, -74.052], [4.58, -74.058], [4.54, -74.08],
  [4.48, -74.09], [4.48, -74.0], [4.78, -74.0]];
const RIO: Par[] = [[4.78, -74.1], [4.74, -74.13], [4.71, -74.17], [4.68, -74.19], [4.65, -74.21], [4.62, -74.225],
  [4.59, -74.215], [4.56, -74.23], [4.53, -74.245]];
const VIAS: Par[][] = [
  [[4.775, -74.045], [4.68, -74.058]],
  [[4.68, -74.058], [4.62, -74.068], [4.57, -74.1], [4.5, -74.115]],
  [[4.775, -74.03], [4.7, -74.035], [4.64, -74.06], [4.6, -74.07]],
  [[4.7, -74.065], [4.64, -74.083], [4.6, -74.11], [4.58, -74.16], [4.59, -74.21]],
  [[4.77, -74.07], [4.7, -74.115], [4.64, -74.14], [4.6, -74.15], [4.55, -74.15]],
  [[4.665, -74.06], [4.7, -74.1], [4.735, -74.15]],
  [[4.615, -74.07], [4.65, -74.11], [4.69, -74.15]],
  [[4.615, -74.1], [4.63, -74.15], [4.625, -74.19]],
  [[4.605, -74.085], [4.645, -74.145], [4.69, -74.205]],
  [[4.74, -74.1], [4.66, -74.14], [4.62, -74.165], [4.6, -74.18]],
];
const LOCALIDADES: [string, number, number][] = [["Usaquén", 4.711, -74.03], ["Chapinero", 4.648, -74.062], ["Santa Fe", 4.61, -74.07],
  ["San Cristóbal", 4.565, -74.088], ["Usme", 4.5, -74.11], ["Tunjuelito", 4.576, -74.133],
  ["Bosa", 4.618, -74.188], ["Kennedy", 4.63, -74.155], ["Fontibón", 4.674, -74.145],
  ["Engativá", 4.707, -74.11], ["Suba", 4.742, -74.084], ["Barrios Unidos", 4.668, -74.076],
  ["Teusaquillo", 4.638, -74.085], ["Los Mártires", 4.605, -74.092], ["Antonio Nariño", 4.588, -74.102],
  ["Puente Aranda", 4.617, -74.117], ["Rafael Uribe", 4.57, -74.116], ["Ciudad Bolívar", 4.545, -74.15],
  ["Soacha", 4.579, -74.217], ["Funza", 4.716, -74.211]];

const x = (lng: number) => ((lng - LIMITES.lngMin) / (LIMITES.lngMax - LIMITES.lngMin)) * ANCHO;
const y = (lat: number) => ((LIMITES.latMax - lat) / (LIMITES.latMax - LIMITES.latMin)) * ALTO;
const xy = (p: PuntoMapa | Par) =>
  Array.isArray(p) ? `${x(p[1]).toFixed(1)},${y(p[0]).toFixed(1)}` : `${x(p.longitud).toFixed(1)},${y(p.latitud).toFixed(1)}`;
const esc = (t: unknown) =>
  String(t ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

export const colorRuta = (indice: number) => COLORES[indice % COLORES.length];

/** Solo una ruta en curso con coordenadas tiene un vehiculo que mostrar. */
export const tieneUbicacion = (u: Ubicacion): u is Ubicacion & { vehiculo: PuntoMapa } =>
  u.estado === "EN_CURSO" && Number.isFinite(u.vehiculo?.latitud) && Number.isFinite(u.vehiculo?.longitud);

export function mensajeSinUbicacion(u: Ubicacion): string | null {
  if (tieneUbicacion(u)) return null;
  return u.estado === "EN_CURSO" ? "Sin datos de ubicación todavía" : "La ruta no está en curso";
}

function fondo(escala: number): string {
  const p: string[] = [`<rect x="0" y="0" width="${ANCHO}" height="${ALTO}" fill="#e9edf1"/>`];
  for (let lat = 4.5; lat < LIMITES.latMax; lat += 0.02)
    p.push(`<line x1="0" x2="${ANCHO}" y1="${y(lat).toFixed(1)}" y2="${y(lat).toFixed(1)}" stroke="#dfe4ea" stroke-width="${escala}"/>`);
  for (let lng = -74.24; lng < LIMITES.lngMax; lng += 0.02)
    p.push(`<line y1="0" y2="${ALTO}" x1="${x(lng).toFixed(1)}" x2="${x(lng).toFixed(1)}" stroke="#dfe4ea" stroke-width="${escala}"/>`);
  p.push(`<polygon points="${AREA_URBANA.map(xy).join(" ")}" fill="#f8f9fa" stroke="#cfd6de" stroke-width="${1.5 * escala}"/>`);
  p.push(`<polygon points="${CERROS.map(xy).join(" ")}" fill="#dbe5d6" stroke="#c4d2bd" stroke-width="${escala}"/>`);
  p.push(`<polyline points="${RIO.map(xy).join(" ")}" fill="none" stroke="#a9c7e3" stroke-width="${5 * escala}" stroke-linecap="round" stroke-linejoin="round"/>`);
  VIAS.forEach((v) => p.push(`<polyline points="${v.map(xy).join(" ")}" fill="none" stroke="#fff" stroke-width="${6 * escala}" stroke-linecap="round" stroke-linejoin="round"/>`));
  VIAS.forEach((v) => p.push(`<polyline points="${v.map(xy).join(" ")}" fill="none" stroke="#d3d9e0" stroke-width="${1.2 * escala}" stroke-linecap="round" stroke-linejoin="round"/>`));
  LOCALIDADES.forEach(([nombre, lat, lng]) =>
    p.push(`<text x="${x(lng).toFixed(1)}" y="${y(lat).toFixed(1)}" text-anchor="middle" font-size="${13 * escala}" fill="#8b97a8" font-weight="600" letter-spacing="${0.6 * escala}">${esc(nombre.toUpperCase())}</text>`));
  return p.join("");
}

function cedi(ubicaciones: Ubicacion[], escala: number): string {
  const o = ubicaciones.length ? ubicaciones[0].origen : { latitud: 4.633, longitud: -74.118 };
  const cx = x(o.longitud), cy = y(o.latitud), t = 11 * escala;
  return `<g><rect x="${cx - t}" y="${cy - t}" width="${2 * t}" height="${2 * t}" rx="${3 * escala}" fill="#102741"/>`
    + `<text x="${cx}" y="${cy + 4 * escala}" text-anchor="middle" font-size="${10 * escala}" fill="#fff" font-weight="700">CD</text>`
    + `<text x="${cx}" y="${cy + t + 15 * escala}" text-anchor="middle" font-size="${12 * escala}" fill="#102741" font-weight="700">Centro de distribución</text></g>`;
}

function ruta(u: Ubicacion & { vehiculo: PuntoMapa }, tono: string, escala: number, resaltada: boolean, atenuada: boolean): string {
  const puntos = [u.origen, ...u.paradas, u.destino];
  const recorridos = Math.min(u.entregasRegistradas, u.paradas.length);
  const p = [`<g opacity="${atenuada ? 0.25 : 1}" data-ruta="${u.rutaId}" style="cursor:pointer">`];
  p.push(`<polyline points="${puntos.map(xy).join(" ")}" fill="none" stroke="${tono}" stroke-width="${(resaltada ? 3 : 2) * escala}" stroke-dasharray="${6 * escala} ${5 * escala}" stroke-opacity=".55" stroke-linejoin="round"/>`);
  const hecho = [...puntos.slice(0, recorridos + 1), u.vehiculo];
  p.push(`<polyline points="${hecho.map(xy).join(" ")}" fill="none" stroke="${tono}" stroke-width="${(resaltada ? 4.5 : 3) * escala}" stroke-linejoin="round" stroke-linecap="round"/>`);
  u.paradas.forEach((parada, i) => {
    const relleno = parada.estado === "ENTREGADA" ? "#0f7b4f" : parada.estado === "NO_ENTREGADA" ? "#c0392b" : "#ffffff";
    p.push(`<circle cx="${x(parada.longitud).toFixed(1)}" cy="${y(parada.latitud).toFixed(1)}" r="${5 * escala}" fill="${relleno}" stroke="${parada.estado === "PENDIENTE" ? tono : "#fff"}" stroke-width="${1.8 * escala}"><title>${esc(`${u.codigo} · ${parada.etiqueta} · ${(parada.estado ?? "").replace("_", " ")}`)}</title></circle>`);
    if (resaltada && u.paradas.length <= 30)
      p.push(`<text x="${(x(parada.longitud) + 8 * escala).toFixed(1)}" y="${(y(parada.latitud) - 6 * escala).toFixed(1)}" font-size="${10 * escala}" fill="#44526a">${i + 1}</text>`);
  });
  const d = u.destino;
  p.push(`<path transform="translate(${x(d.longitud).toFixed(1)} ${y(d.latitud).toFixed(1)}) scale(${escala})" d="M0 0 L0 -22 L14 -17 L0 -12" fill="${tono}" stroke="${tono}" stroke-width="2" stroke-linejoin="round"><title>${esc("Destino: " + d.etiqueta)}</title></path>`);
  p.push("</g>");
  return p.join("");
}

function vehiculo(u: Ubicacion & { vehiculo: PuntoMapa }, tono: string, escala: number): string {
  const cx = x(u.vehiculo.longitud), cy = y(u.vehiculo.latitud);
  const texto = u.vehiculo.etiqueta;
  const ancho = (texto.length * 7.2 + 16) * escala, alto = 20 * escala;
  const pulso = (u.velocidadKmh ?? 0) > 0
    ? `<circle class="pulso" cx="${cx.toFixed(1)}" cy="${cy.toFixed(1)}" r="9" fill="${tono}" style="transform-box: fill-box"/>` : "";
  return `<g data-ruta="${u.rutaId}" style="cursor:pointer">${pulso}`
    + `<circle cx="${cx.toFixed(1)}" cy="${cy.toFixed(1)}" r="${7 * escala}" fill="${tono}" stroke="#fff" stroke-width="${2.5 * escala}"/>`
    + `<rect x="${(cx - ancho / 2).toFixed(1)}" y="${(cy - alto - 12 * escala).toFixed(1)}" width="${ancho.toFixed(1)}" height="${alto.toFixed(1)}" rx="${3 * escala}" fill="#102741"/>`
    + `<text x="${cx.toFixed(1)}" y="${(cy - 18 * escala).toFixed(1)}" text-anchor="middle" font-size="${11.5 * escala}" fill="#fff" font-weight="700" letter-spacing="${0.5 * escala}">${esc(texto)}</text>`
    + `<title>${esc(`${u.codigo} · ${u.descripcionPosicion}`)}</title></g>`;
}

function caja(ubicaciones: (Ubicacion & { vehiculo: PuntoMapa })[]): [number, number, number, number] {
  const puntos = ubicaciones.flatMap((u) => [u.origen, u.destino, u.vehiculo, ...u.paradas]);
  if (!puntos.length) return [0, 0, ANCHO, ALTO];
  const xs = puntos.map((p) => x(p.longitud)), ys = puntos.map((p) => y(p.latitud));
  const minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
  const margen = 60;
  let ancho = Math.max(maxX - minX + margen * 2, 320), alto = Math.max(maxY - minY + margen * 2, 240);
  if (ancho / alto < 1.35) ancho = alto * 1.35; else alto = ancho / 1.35;
  const cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
  return [cx - ancho / 2, cy - alto / 2, ancho, alto];
}

export interface OpcionesMapa {
  enfocar?: boolean;
  seleccion?: number | null;
  colores?: Record<number, string>;
}

/** Devuelve el SVG del plano. Todo texto dinamico se escapa con esc(). */
export function svgMapa(rutasRecibidas: Ubicacion[], opciones: OpcionesMapa = {}): string {
  const ubicaciones = rutasRecibidas.filter(tieneUbicacion);
  const vista = opciones.enfocar ? caja(ubicaciones) : ([0, 0, ANCHO, ALTO] as [number, number, number, number]);
  const escala = Math.max(0.45, vista[2] / ANCHO);
  const seleccion = opciones.seleccion ?? null;
  const colores = opciones.colores ?? {};
  const rutas = ubicaciones
    .map((u, i) => ruta(u, colores[u.rutaId] || colorRuta(i), escala, seleccion === u.rutaId || ubicaciones.length === 1, seleccion !== null && seleccion !== u.rutaId))
    .join("");
  const vehiculos = ubicaciones
    .filter((u) => seleccion === null || seleccion === u.rutaId)
    .map((u) => vehiculo(u, colores[u.rutaId] || colorRuta(ubicaciones.indexOf(u)), escala))
    .join("");
  const nx = vista[0] + vista[2] - 40 * escala, ny = vista[1] + 50 * escala;
  const brujula = `<g transform="translate(${nx} ${ny}) scale(${escala})"><circle r="18" fill="#fff" stroke="#c9d2de"/><path d="M0 -13 L6 5 L0 1 L-6 5 Z" fill="#102741"/><text y="-21" text-anchor="middle" font-size="11" font-weight="700" fill="#102741">N</text></g>`;
  const ex = vista[0] + 24 * escala, ey = vista[1] + vista[3] - 24 * escala, largo = x(-74.18) - x(-74.2);
  const regla = `<g><rect x="${ex}" y="${ey - 6 * escala}" width="${largo}" height="${6 * escala}" fill="#102741"/><rect x="${ex + largo / 2}" y="${ey - 6 * escala}" width="${largo / 2}" height="${6 * escala}" fill="#fff" stroke="#102741" stroke-width="${escala}"/>`
    + `<text x="${ex}" y="${ey - 11 * escala}" font-size="${11 * escala}" fill="#44526a">0</text><text x="${ex + largo}" y="${ey - 11 * escala}" font-size="${11 * escala}" fill="#44526a" text-anchor="end">2,2 km</text></g>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${vista.map((v) => v.toFixed(1)).join(" ")}" role="img" aria-label="Mapa de ubicación simulada" font-family="Segoe UI, Arial, sans-serif">`
    + fondo(escala) + cedi(ubicaciones, escala) + rutas + vehiculos + brujula + regla + "</svg>";
}
