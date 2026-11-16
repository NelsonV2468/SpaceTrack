"use client";

import { createContext, useContext } from "react";
import type { Rol, UsuarioSesion } from "./tipos";

export interface Permisos {
  usuario: UsuarioSesion;
  puedeEscribir: boolean;
  esAdmin: boolean;
  salir: () => Promise<void>;
}

export const ContextoSesion = createContext<Permisos | null>(null);

export function useSesion(): Permisos {
  const valor = useContext(ContextoSesion);
  if (!valor) throw new Error("useSesion debe usarse dentro del panel");
  return valor;
}

export const NOMBRE_ROL: Record<Rol, string> = {
  ADMIN: "Administrador",
  OPERADOR: "Operador",
  CONSULTA: "Consulta",
  PENDIENTE: "Pendiente",
};
