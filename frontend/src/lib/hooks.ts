"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { apiGet } from "./api";

interface Carga<T> {
  datos: T | undefined;
  error: string | null;
  cargando: boolean;
  recargar: () => Promise<void>;
}

/** Carga un recurso GET de la API. Con `refrescoMs` se vuelve a pedir periodicamente (mapa en vivo). */
export function useRecurso<T>(url: string | null, refrescoMs?: number): Carga<T> {
  const [datos, setDatos] = useState<T>();
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(Boolean(url));
  const vigente = useRef(url);
  vigente.current = url;

  const recargar = useCallback(async () => {
    if (!url) return;
    try {
      const resultado = await apiGet<T>(url);
      if (vigente.current === url) {
        setDatos(resultado);
        setError(null);
      }
    } catch (e) {
      if (vigente.current === url) setError(e instanceof Error ? e.message : "Error al cargar");
    } finally {
      if (vigente.current === url) setCargando(false);
    }
  }, [url]);

  useEffect(() => {
    setCargando(Boolean(url));
    void recargar();
    if (!url || !refrescoMs) return;
    const id = setInterval(() => {
      // No refresca mientras hay una ventana modal abierta para no interrumpir formularios.
      if (!document.querySelector("[data-modal-abierto]")) void recargar();
    }, refrescoMs);
    return () => clearInterval(id);
  }, [url, refrescoMs, recargar]);

  return { datos, error, cargando, recargar };
}
