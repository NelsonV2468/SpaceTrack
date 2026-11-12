// Configuracion de los modulos CRUD. Para agregar uno nuevo basta con declarar aqui su API,
// columnas y campos; la pagina generica (ModuloCrud) se encarga del resto.
import type { ReactNode } from "react";
import type { CampoDef } from "@/components/Formulario";
import type { NombreIcono } from "@/components/Icono";
import { Activo, BarraEntregas, DiasSemanaChips, Doble, Etiqueta, EstadoRutaEtiqueta, SiNo } from "@/components/ui";
import {
  TIPOS_VEHICULO, TONO_OPERACION, capitalizar, fecha, fechaHora, hora, hoy, licenciaVigente, miles, nombreConductor,
} from "@/lib/formato";
import type { Cliente, Conductor, Necesidad, Operacion, Ruta, Vehiculo, Zona } from "@/lib/tipos";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Cualquiera = any;

export interface ModuloDef<T = Cualquiera> {
  clave: string;
  grupo: "Gestión" | "Planeación";
  titulo: string;
  menu?: string;
  singular: string;
  femenino?: boolean;
  api: string;
  icono: NombreIcono;
  descripcion: string;
  columnas: [string, (item: T) => ReactNode][];
  campos: CampoDef[];
  filtro?: { parametro: string; etiqueta: string; opciones: [string, string][] };
  /** Clic en la fila: ruta a la que navega (p. ej. detalle de ruta). */
  enlaceFila?: (item: T) => string;
  /** Las rutas solo se editan/eliminan desde su detalle. */
  soloDetalle?: boolean;
  alias?: Record<string, string>;
}

const clientes: ModuloDef<Cliente> = {
  clave: "clientes", grupo: "Gestión", titulo: "Clientes", singular: "cliente", api: "/api/clientes", icono: "clientes",
  descripcion: "Empresas contratantes del servicio de distribución.",
  columnas: [
    ["Empresa", (c) => <Doble principal={c.razonSocial} secundario={`NIT ${c.nit}`} />],
    ["Contacto", (c) => <Doble principal={c.nombreContacto} secundario={`${c.correoContacto} · ${c.telefonoContacto}`} />],
    ["Dirección", (c) => c.direccion],
    ["Estado", (c) => <Activo valor={c.activo} />],
  ],
  campos: [
    { nombre: "razonSocial", etiqueta: "Razón social", ancho: true },
    { nombre: "nit", etiqueta: "NIT", ayuda: "Ej.: 900123456-7" },
    { nombre: "nombreContacto", etiqueta: "Nombre de contacto" },
    { nombre: "correoContacto", etiqueta: "Correo de contacto", tipo: "email" },
    { nombre: "telefonoContacto", etiqueta: "Teléfono de contacto" },
    { nombre: "direccion", etiqueta: "Dirección", ancho: true },
    { nombre: "activo", etiqueta: "Cliente activo", tipo: "check", valor: true },
  ],
};

const vehiculos: ModuloDef<Vehiculo> = {
  clave: "vehiculos", grupo: "Gestión", titulo: "Vehículos", singular: "vehículo", api: "/api/vehiculos", icono: "vehiculos",
  descripcion: "Flota disponible para asignar a las rutas.",
  columnas: [
    ["Placa", (v) => <strong className="num">{v.placa}</strong>],
    ["Vehículo", (v) => <Doble principal={v.tipoVehiculo} secundario={`${v.marca} ${v.modelo} · ${v.anioModelo}`} />],
    ["Capacidad", (v) => `${miles(v.capacidadCargaKg)} kg`],
    ["Cadena de frío", (v) => <SiNo valor={v.requiereCadenaFrio} />],
    ["Estado", (v) => <Activo valor={v.activo} />],
  ],
  campos: [
    { nombre: "placa", etiqueta: "Placa", ayuda: "Ej.: ABC123" },
    { nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo", tipo: "select", opciones: TIPOS_VEHICULO },
    { nombre: "marca", etiqueta: "Marca" },
    { nombre: "modelo", etiqueta: "Modelo" },
    { nombre: "anioModelo", etiqueta: "Año del modelo", tipo: "number", valor: new Date().getFullYear() },
    { nombre: "capacidadCargaKg", etiqueta: "Capacidad de carga (kg)", tipo: "number" },
    { nombre: "requiereCadenaFrio", etiqueta: "Cuenta con cadena de frío", tipo: "check" },
    { nombre: "activo", etiqueta: "Vehículo activo", tipo: "check", valor: true },
  ],
};

const conductores: ModuloDef<Conductor> = {
  clave: "conductores", grupo: "Gestión", titulo: "Conductores", singular: "conductor", api: "/api/conductores", icono: "conductores",
  descripcion: "Personal habilitado para operar la flota.",
  columnas: [
    ["Conductor", (c) => <Doble principal={nombreConductor(c)} secundario={`${c.tipoDocumento} ${c.numeroDocumento}`} />],
    ["Contacto", (c) => <Doble principal={c.telefono} secundario={c.correo} />],
    ["Licencia", (c) => <Doble principal={c.numeroLicencia} secundario={`Vence ${fecha(c.fechaVencimientoLicencia)}`} />],
    ["Habilitación", (c) => !c.activo ? <Etiqueta tono="neutral">Inactivo</Etiqueta>
      : licenciaVigente(c) ? <Etiqueta tono="ok">Habilitado</Etiqueta> : <Etiqueta tono="danger">Licencia vencida</Etiqueta>],
  ],
  campos: [
    { nombre: "tipoDocumento", etiqueta: "Tipo de documento", tipo: "select", opciones: ["CC", "CE", "PASAPORTE"] },
    { nombre: "numeroDocumento", etiqueta: "Número de documento" },
    { nombre: "nombres", etiqueta: "Nombres" },
    { nombre: "apellidos", etiqueta: "Apellidos" },
    { nombre: "telefono", etiqueta: "Teléfono" },
    { nombre: "correo", etiqueta: "Correo", tipo: "email" },
    { nombre: "numeroLicencia", etiqueta: "Número de licencia" },
    { nombre: "fechaVencimientoLicencia", etiqueta: "Vencimiento de la licencia", tipo: "date" },
    { nombre: "activo", etiqueta: "Conductor activo", tipo: "check", valor: true },
  ],
};

const operaciones: ModuloDef<Operacion> = {
  clave: "operaciones", grupo: "Gestión", titulo: "Operaciones", singular: "operación", femenino: true, api: "/api/operaciones", icono: "operaciones",
  descripcion: "Contratos de distribución suscritos con cada cliente.",
  alias: { periodoValido: "fechaFin" },
  columnas: [
    ["Contrato", (o) => <Doble principal={o.codigoContrato} secundario={o.nombreOperacion} />],
    ["Cliente", (o) => o.razonSocialCliente],
    ["Vigencia", (o) => <Doble principal={`${fecha(o.fechaInicio)} – ${fecha(o.fechaFin)}`} secundario={o.diasOperacion} />],
    ["Alcance", (o) => <Doble principal={`${o.numeroVehiculosRequeridos} vehículos`} secundario={`${miles(o.numeroEntregasEsperadas)} entregas esperadas`} />],
    ["Estado", (o) => <Etiqueta tono={TONO_OPERACION[o.estado] ?? "neutral"}>{capitalizar(o.estado)}</Etiqueta>],
  ],
  campos: [
    { nombre: "codigoContrato", etiqueta: "Código de contrato", ayuda: "Ej.: CT-2026-001" },
    { nombre: "clienteId", etiqueta: "Cliente", tipo: "relacion", fuente: "clientes", mostrar: (c) => String(c.razonSocial) },
    { nombre: "nombreOperacion", etiqueta: "Nombre de la operación", ancho: true },
    { nombre: "fechaInicio", etiqueta: "Fecha de inicio", tipo: "date", valor: hoy(0) },
    { nombre: "fechaFin", etiqueta: "Fecha de finalización", tipo: "date", valor: hoy(90) },
    { nombre: "diasOperacion", etiqueta: "Días de operación (resumen)", valor: "Lunes a sábado" },
    { nombre: "estado", etiqueta: "Estado", tipo: "select", opciones: [["ACTIVA", "Activa"], ["PAUSADA", "Pausada"], ["FINALIZADA", "Finalizada"]] },
    { nombre: "numeroVehiculosRequeridos", etiqueta: "Vehículos requeridos", tipo: "number", valor: 1 },
    { nombre: "numeroEntregasEsperadas", etiqueta: "Entregas esperadas", tipo: "number", valor: 1 },
    { nombre: "requiereCadenaFrio", etiqueta: "Requiere cadena de frío", tipo: "check" },
  ],
};

const necesidades: ModuloDef<Necesidad> = {
  clave: "necesidades", grupo: "Planeación", titulo: "Necesidades del cliente", menu: "Necesidades", singular: "necesidad", femenino: true,
  api: "/api/necesidades", icono: "necesidades",
  descripcion: "Vehículos, días y franja horaria que requiere cada contrato.",
  alias: { horarioValido: "horaFin" },
  columnas: [
    ["Contrato", (n) => <Doble principal={n.codigoContrato} secundario={n.razonSocialCliente} />],
    ["Vehículos requeridos", (n) => <Doble principal={`${n.cantidadVehiculos} × ${n.tipoVehiculo}`}
      secundario={`Mín. ${miles(n.capacidadMinimaKg)} kg${n.requiereCadenaFrio ? " · cadena de frío" : ""}`} />],
    ["Días", (n) => <DiasSemanaChips lista={n.diasOperacion} />],
    ["Horario", (n) => <span className="num">{hora(n.horaInicio)} – {hora(n.horaFin)}</span>],
    ["Cobertura de flota", (n) => <Etiqueta tono={n.flotaSuficiente ? "ok" : "warn"}>{n.vehiculosDisponibles} de {n.cantidadVehiculos} disponibles</Etiqueta>],
  ],
  campos: [
    { nombre: "operacionId", etiqueta: "Operación / contrato", tipo: "relacion", fuente: "operaciones", ancho: true,
      mostrar: (o) => `${o.codigoContrato} — ${o.razonSocialCliente}` },
    { nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo", tipo: "select", opciones: TIPOS_VEHICULO },
    { nombre: "cantidadVehiculos", etiqueta: "Cantidad de vehículos", tipo: "number", valor: 1 },
    { nombre: "capacidadMinimaKg", etiqueta: "Capacidad mínima (kg)", tipo: "number" },
    { nombre: "requiereCadenaFrio", etiqueta: "Requiere cadena de frío", tipo: "check" },
    { nombre: "diasOperacion", etiqueta: "Días de operación", tipo: "dias", ancho: true },
    { nombre: "horaInicio", etiqueta: "Hora de inicio", tipo: "time", valor: "06:00" },
    { nombre: "horaFin", etiqueta: "Hora de finalización", tipo: "time", valor: "14:00" },
    { nombre: "observaciones", etiqueta: "Observaciones", tipo: "textarea", ancho: true, opcional: true },
  ],
};

const zonas: ModuloDef<Zona> = {
  clave: "zonas", grupo: "Planeación", titulo: "Zonas", singular: "zona", femenino: true, api: "/api/zonas", icono: "zonas",
  descripcion: "Áreas de la ciudad cubiertas por las rutas. La localidad define la ubicación en el mapa.",
  columnas: [
    ["Zona", (z) => <Doble principal={z.nombre} secundario={z.codigo} />],
    ["Localidad", (z) => z.localidad],
    ["Descripción", (z) => z.descripcion || "—"],
    ["Estado", (z) => <Activo valor={z.activa} femenino />],
  ],
  campos: [
    { nombre: "codigo", etiqueta: "Código", ayuda: "Ej.: Z-NORTE" },
    { nombre: "nombre", etiqueta: "Nombre" },
    { nombre: "localidad", etiqueta: "Localidad", ayuda: "Ej.: Usaquén, Kennedy, Suba, Chapinero" },
    { nombre: "descripcion", etiqueta: "Descripción", opcional: true },
    { nombre: "activa", etiqueta: "Zona activa", tipo: "check", valor: true },
  ],
};

export const CAMPOS_RUTA: CampoDef[] = [
  { nombre: "codigo", etiqueta: "Código", ayuda: "Ej.: R-001" },
  { nombre: "nombre", etiqueta: "Nombre" },
  { nombre: "zonaId", etiqueta: "Zona", tipo: "relacion", fuente: "zonas", filtrar: (z) => Boolean(z.activa),
    mostrar: (z) => `${z.nombre} (${z.localidad})` },
  { nombre: "operacionId", etiqueta: "Operación / contrato", tipo: "relacion", fuente: "operaciones", filtrar: (o) => o.estado !== "FINALIZADA",
    mostrar: (o) => `${o.codigoContrato} — ${o.razonSocialCliente}` },
  { nombre: "puntoOrigen", etiqueta: "Punto de origen", valor: "CEDI Puente Aranda" },
  { nombre: "puntoDestino", etiqueta: "Punto de destino" },
  { nombre: "distanciaEstimadaKm", etiqueta: "Distancia estimada (km)", tipo: "number" },
  { nombre: "numeroEntregasProgramadas", etiqueta: "Entregas programadas", tipo: "number", valor: 1 },
];

const rutas: ModuloDef<Ruta> = {
  clave: "rutas", grupo: "Planeación", titulo: "Rutas", singular: "ruta", femenino: true, api: "/api/rutas", icono: "rutas",
  descripcion: "Recorridos de distribución con sus recursos, entregas programadas y estado.",
  alias: { recorridoValido: "puntoDestino" },
  soloDetalle: true,
  enlaceFila: (r) => `/panel/rutas/detalle/?id=${r.id}`,
  filtro: { parametro: "estado", etiqueta: "Estado", opciones: [["PENDIENTE", "Pendiente"], ["EN_CURSO", "En curso"], ["FINALIZADA", "Finalizada"]] },
  columnas: [
    ["Ruta", (r) => <Doble principal={r.codigo} secundario={r.nombre} />],
    ["Zona / contrato", (r) => <Doble principal={r.nombreZona} secundario={r.codigoContrato} />],
    ["Vehículo y conductor", (r) => r.placaVehiculo ? <Doble principal={r.placaVehiculo} secundario={r.nombreConductor} /> : <Etiqueta tono="warn">Sin asignar</Etiqueta>],
    ["Entregas", (r) => <BarraEntregas realizadas={r.entregasRealizadas} noRealizadas={r.entregasNoRealizadas} programadas={r.numeroEntregasProgramadas} />],
    ["Estado", (r) => <Doble principal={<EstadoRutaEtiqueta estado={r.estado} />}
      secundario={r.estado === "EN_CURSO" ? `Desde ${fechaHora(r.fechaInicioReal)}` : r.estado === "FINALIZADA" ? `Cerrada ${fechaHora(r.fechaFinReal)}` : undefined} />],
  ],
  campos: CAMPOS_RUTA,
};

export const MODULOS: Record<string, ModuloDef> = { clientes, vehiculos, conductores, operaciones, necesidades, zonas, rutas };
export const CLAVES_MODULOS = Object.keys(MODULOS);
