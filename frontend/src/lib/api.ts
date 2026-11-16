// Cliente HTTP de la API. Misma sesion que el backend: cookie HttpOnly + cabecera CSRF.

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly estado: number,
    public readonly campos: Record<string, string> = {},
  ) {
    super(message);
  }
}

const METODOS_SEGUROS = new Set(["GET", "HEAD", "OPTIONS"]);

export function tokenCsrf(): string {
  if (typeof document === "undefined") return "";
  const encontrado = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return encontrado ? decodeURIComponent(encontrado[1]) : "";
}

let alExpirarSesion: (() => void) | null = null;
/** El layout del panel registra aqui la redireccion al login cuando la sesion caduca. */
export function configurarExpiracion(callback: () => void) {
  alExpirarSesion = callback;
}

export async function api<T = unknown>(metodo: string, url: string, cuerpo?: unknown): Promise<T> {
  const cabeceras: Record<string, string> = { Accept: "application/json" };
  if (cuerpo !== undefined) cabeceras["Content-Type"] = "application/json";
  if (!METODOS_SEGUROS.has(metodo)) cabeceras["X-XSRF-TOKEN"] = tokenCsrf();

  let respuesta: Response;
  try {
    respuesta = await fetch(url, {
      method: metodo,
      headers: cabeceras,
      credentials: "same-origin",
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ApiError("No hay conexión con el servidor. Intente de nuevo en unos segundos.", 0);
  }

  if (respuesta.status === 401) alExpirarSesion?.();
  if (respuesta.status === 204) return null as T;
  const datos = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    throw new ApiError(
      datos.mensaje || `No se pudo completar la operación (${respuesta.status})`,
      respuesta.status,
      datos.errores || {},
    );
  }
  return datos as T;
}

export const apiGet = <T,>(url: string) => api<T>("GET", url);
