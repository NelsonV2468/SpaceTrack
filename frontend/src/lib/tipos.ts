// Contratos de la API REST de SpaceTrack (mismos nombres de campo que los DTO de Spring).

export type Rol = "ADMIN" | "OPERADOR" | "CONSULTA" | "PENDIENTE";
export type EstadoRuta = "PENDIENTE" | "EN_CURSO" | "FINALIZADA";
export type EstadoEntrega = "ENTREGADA" | "NO_ENTREGADA";
export type EstadoOperacion = "ACTIVA" | "PAUSADA" | "FINALIZADA";
export type DiaSemana = "LUNES" | "MARTES" | "MIERCOLES" | "JUEVES" | "VIERNES" | "SABADO" | "DOMINGO";

export interface UsuarioSesion {
  id: number | null;
  correo: string;
  nombre: string;
  rol: Rol;
}

export interface Usuario {
  id: number;
  correo: string;
  nombre: string;
  rol: Rol;
  fechaCreacion: string;
  ultimoIngreso: string | null;
}

export interface Cliente {
  id: number;
  razonSocial: string;
  nit: string;
  nombreContacto: string;
  correoContacto: string;
  telefonoContacto: string;
  direccion: string;
  activo: boolean;
}

export interface Vehiculo {
  id: number;
  placa: string;
  tipoVehiculo: string;
  marca: string;
  modelo: string;
  anioModelo: number;
  capacidadCargaKg: number;
  requiereCadenaFrio: boolean;
  activo: boolean;
}

export interface Conductor {
  id: number;
  tipoDocumento: string;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  numeroLicencia: string;
  fechaVencimientoLicencia: string;
  activo: boolean;
}

export interface Operacion {
  id: number;
  codigoContrato: string;
  clienteId: number;
  razonSocialCliente: string;
  nombreOperacion: string;
  fechaInicio: string;
  fechaFin: string;
  diasOperacion: string;
  numeroVehiculosRequeridos: number;
  numeroEntregasEsperadas: number;
  requiereCadenaFrio: boolean;
  estado: EstadoOperacion;
}

export interface Necesidad {
  id: number;
  operacionId: number;
  codigoContrato: string;
  razonSocialCliente: string;
  tipoVehiculo: string;
  cantidadVehiculos: number;
  capacidadMinimaKg: number;
  requiereCadenaFrio: boolean;
  diasOperacion: DiaSemana[];
  horaInicio: string;
  horaFin: string;
  observaciones: string | null;
  vehiculosDisponibles: number;
  flotaSuficiente: boolean;
}

export interface Zona {
  id: number;
  codigo: string;
  nombre: string;
  localidad: string;
  descripcion: string | null;
  activa: boolean;
  /** true para las localidades de Bogota predefinidas; false para zonas antiguas registradas a mano. */
  catalogo: boolean;
}

export interface Ruta {
  id: number;
  codigo: string;
  nombre: string;
  zonaId: number;
  nombreZona: string;
  operacionId: number;
  codigoContrato: string;
  razonSocialCliente?: string;
  puntoOrigen: string;
  puntoDestino: string;
  numeroEntregasProgramadas: number;
  entregasRealizadas: number;
  entregasNoRealizadas: number;
  entregasEsperadasContrato: number;
  entregasProgramadasContrato: number;
  estado: EstadoRuta;
  vehiculoId: number | null;
  placaVehiculo: string | null;
  descripcionVehiculo: string | null;
  conductorId: number | null;
  nombreConductor: string | null;
  fechaInicioReal: string | null;
  fechaFinReal: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
  /** Solo llega al finalizar una ruta con entregas faltantes: indica la zona y cuantas entregas faltaron. */
  advertencia?: string | null;
}

export interface Entrega {
  id: number;
  rutaId: number;
  codigoRuta: string;
  numeroGuia: string | null;
  destinatario: string;
  direccion: string;
  estado: EstadoEntrega;
  novedad: string | null;
  fechaRegistro: string;
}

export interface PuntoMapa {
  latitud: number;
  longitud: number;
  etiqueta: string;
  estado?: "ENTREGADA" | "NO_ENTREGADA" | "PENDIENTE";
}

export interface Ubicacion {
  rutaId: number;
  codigo: string;
  nombre: string;
  estado: EstadoRuta;
  placaVehiculo: string | null;
  nombreConductor: string | null;
  origen: PuntoMapa;
  destino: PuntoMapa;
  paradas: PuntoMapa[];
  vehiculo: PuntoMapa | null;
  progreso: number;
  velocidadKmh: number | null;
  descripcionPosicion: string;
  entregasRegistradas: number;
  entregasProgramadas: number;
  calculadoEn: string;
  /** El vehiculo reporto su posicion (GPS o registro manual) y se muestra esa en lugar de la simulada. */
  posicionReportada?: boolean;
  /** La posicion reportada se aleja del recorrido planeado. */
  fueraDeRuta?: boolean;
  distanciaDesvioKm?: number | null;
}

/** ON-4: ruta EN_CURSO proxima a terminar su franja horaria sin completar entregas. */
export type NivelAlerta = "ADVERTENCIA" | "CRITICA";
/** FRANJA_HORARIA: poco tiempo para las entregas pendientes. DESVIO_RUTA: el vehiculo se salio del recorrido. */
export type TipoAlerta = "FRANJA_HORARIA" | "DESVIO_RUTA";

export interface AlertaRuta {
  rutaId: number;
  codigoRuta: string;
  nombreRuta: string;
  nombreZona: string | null;
  localidad: string | null;
  codigoContrato: string | null;
  placaVehiculo: string | null;
  nombreConductor: string | null;
  nivel: NivelAlerta;
  entregasProgramadas: number;
  entregasPendientes: number;
  /** Null cuando la ruta sigue abierta desde un dia anterior. */
  horaFinFranja: string | null;
  /** Negativo cuando la franja ya termino. */
  minutosRestantes: number;
  minutosEstimadosPendientes: number;
  mensaje: string;
  calculadoEn: string;
  tipo: TipoAlerta;
  /** Solo en alertas de desvio: km entre el vehiculo y su recorrido. */
  distanciaDesvioKm: number | null;
}

/** ON-20: ruta dentro del resumen o del cierre de un dia. */
export interface RutaCierre {
  rutaId: number;
  codigoRuta: string;
  nombreRuta: string | null;
  nombreZona: string | null;
  placaVehiculo: string | null;
  nombreConductor: string | null;
  fechaInicioReal: string | null;
  fechaFinReal: string | null;
  entregasProgramadas: number;
  entregasRealizadas: number;
  entregasNoRealizadas: number;
  entregasSinRegistrar: number;
}

/** ON-20: cierre diario. Si cerrado es false es la vista previa calculada en vivo. */
export interface CierreDiario {
  id: number | null;
  fecha: string;
  cerrado: boolean;
  fechaRegistro: string | null;
  registradoPor: string | null;
  observaciones: string | null;
  rutasCerradas: number;
  entregasProgramadas: number;
  entregasRealizadas: number;
  entregasNoRealizadas: number;
  entregasSinRegistrar: number;
  cumplimiento: number;
  rutas: RutaCierre[];
  rutasEnCurso: string[];
  puedeCerrar: boolean;
  motivoNoCierre: string | null;
}
