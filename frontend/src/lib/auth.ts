// Integracion con Neon Auth: el navegador valida la identidad en Neon y luego cambia el JWT
// por una sesion HttpOnly de SpaceTrack (POST /auth/session). No se guardan tokens en el navegador.
import { createAuthClient, type VanillaBetterAuthClient } from "@neon/auth";
import { ApiError, tokenCsrf } from "./api";
import type { UsuarioSesion } from "./tipos";

export interface ConfigAuth {
  seguridad: boolean;
  baseUrl?: string;
}

// Cliente Better Auth de Neon (adaptador vanilla por defecto) para registro, ingreso y salida.
// Es un Proxy: no se le agregan propiedades; la URL base se guarda aparte.
type ClienteNeon = VanillaBetterAuthClient;
const URL_BASE = new WeakMap<object, string>();
function crearCliente(baseUrl: string): ClienteNeon {
  const neon = createAuthClient(baseUrl);
  URL_BASE.set(neon, baseUrl.replace(/\/$/, ""));
  return neon;
}

/** JWT de 15 minutos emitido por Neon Auth para la sesion activa (cookie de Neon). */
async function obtenerJwt(neon: ClienteNeon): Promise<string | null> {
  const base = URL_BASE.get(neon);
  if (!base) return null;
  try {
    const respuesta = await fetch(`${base}/token`, { credentials: "include", headers: { Accept: "application/json" } });
    if (!respuesta.ok) return null;
    const cuerpo = (await respuesta.json()) as { token?: string };
    return typeof cuerpo.token === "string" && cuerpo.token ? cuerpo.token : null;
  } catch {
    return null;
  }
}
let cliente: Promise<{ config: ConfigAuth; neon: ClienteNeon | null }> | null = null;

export function clienteAuth() {
  cliente ??= fetch("/auth/config", { headers: { Accept: "application/json" } })
    .then((r) => {
      if (!r.ok) throw new Error("config");
      return r.json() as Promise<ConfigAuth>;
    })
    .then((config) => ({ config, neon: config.seguridad && config.baseUrl ? crearCliente(config.baseUrl) : null }))
    .catch((e) => {
      cliente = null;
      throw e;
    });
  return cliente;
}

const MENSAJES: Record<string, string> = {
  INVALID_EMAIL_OR_PASSWORD: "Correo o contraseña incorrectos.",
  USER_ALREADY_EXISTS: "Ya existe una cuenta con ese correo.",
  USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL: "Ya existe una cuenta con ese correo.",
  PASSWORD_TOO_SHORT: "La contraseña es demasiado corta.",
  PASSWORD_TOO_LONG: "La contraseña es demasiado larga.",
  INVALID_EMAIL: "El correo no es válido.",
  EMAIL_NOT_VERIFIED: "Debe verificar su correo antes de ingresar.",
  TOO_MANY_REQUESTS: "Demasiados intentos. Espere un momento.",
};

export const SIN_CONEXION = "No hay conexión con el servidor de SpaceTrack. Verifique que esté en línea e intente de nuevo.";

/** Mensaje en español para cualquier fallo del flujo de acceso (incluye "Failed to fetch" del navegador). */
export function mensajeError(error: unknown, porDefecto: string): string {
  if (error instanceof TypeError) return SIN_CONEXION;
  return error instanceof Error && error.message ? error.message : porDefecto;
}

export function traducirErrorNeon(error: { code?: string; message?: string } | null | undefined, porDefecto: string) {
  return (error?.code && MENSAJES[error.code]) || error?.message || porDefecto;
}

/** Cambia la sesion de Neon Auth por la sesion de SpaceTrack. Lanza ApiError(403) si la cuenta esta pendiente. */
export async function abrirSesion(neon: ClienteNeon): Promise<UsuarioSesion> {
  const jwt = await obtenerJwt(neon);
  if (!jwt) throw new ApiError("No fue posible obtener el token de Neon Auth. Inicie sesión de nuevo.", 401);
  let respuesta: Response;
  try {
    respuesta = await fetch("/auth/session", {
      method: "POST",
      credentials: "same-origin",
      headers: { Authorization: `Bearer ${jwt}`, "X-XSRF-TOKEN": tokenCsrf(), Accept: "application/json" },
    });
  } catch {
    throw new ApiError(SIN_CONEXION, 0);
  }
  const cuerpo = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) throw new ApiError(cuerpo.mensaje || "No fue posible iniciar sesión.", respuesta.status);
  return cuerpo as UsuarioSesion;
}

/** Asegura que exista la cookie XSRF-TOKEN antes del primer POST (la emite cualquier GET al backend). */
export async function prepararCsrf() {
  if (!tokenCsrf()) await fetch("/auth/config", { credentials: "same-origin" }).catch(() => undefined);
}
