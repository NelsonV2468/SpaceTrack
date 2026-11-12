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
  distanciaEstimadaKm: number;
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
}
