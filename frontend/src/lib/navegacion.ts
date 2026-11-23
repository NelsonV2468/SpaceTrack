import type { NombreIcono } from "@/components/Icono";

export interface ItemMenu {
  href: string;
  texto: string;
  icono: NombreIcono;
  soloAdmin?: boolean;
  /** Muestra junto al texto el numero de alertas vigentes (ON-4). */
  contadorAlertas?: boolean;
}

export const MENU: { grupo: string; items: ItemMenu[] }[] = [
  { grupo: "General", items: [
    { href: "/panel/", texto: "Tablero", icono: "tablero" },
    { href: "/panel/monitoreo/", texto: "Monitoreo en mapa", icono: "monitoreo" },
  ] },
  { grupo: "Gestión", items: [
    { href: "/panel/clientes/", texto: "Clientes", icono: "clientes" },
    { href: "/panel/vehiculos/", texto: "Vehículos", icono: "vehiculos" },
    { href: "/panel/conductores/", texto: "Conductores", icono: "conductores" },
    { href: "/panel/operaciones/", texto: "Operaciones", icono: "operaciones" },
  ] },
  { grupo: "Planeación", items: [
    { href: "/panel/necesidades/", texto: "Necesidades", icono: "necesidades" },
    { href: "/panel/rutas/", texto: "Rutas", icono: "rutas" },
  ] },
  { grupo: "Ejecución", items: [
    { href: "/panel/entregas/", texto: "Entregas", icono: "entregas" },
    { href: "/panel/alertas/", texto: "Alertas", icono: "alertas", contadorAlertas: true },
    { href: "/panel/cierres/", texto: "Cierre diario", icono: "cierre" },
  ] },
  { grupo: "Administración", items: [
    { href: "/panel/usuarios/", texto: "Usuarios", icono: "usuarios", soloAdmin: true },
  ] },
];

/** Item activo para una ruta: el mas especifico que la contenga (/panel/rutas/detalle -> Rutas). */
export function itemActivo(ruta: string): ItemMenu | undefined {
  const normal = ruta.endsWith("/") ? ruta : `${ruta}/`;
  return MENU.flatMap((g) => g.items)
    .filter((i) => i.href === "/panel/" ? normal === "/panel/" : normal.startsWith(i.href))
    .sort((a, b) => b.href.length - a.href.length)[0];
}
