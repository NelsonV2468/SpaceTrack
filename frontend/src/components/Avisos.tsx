"use client";

import { createContext, useCallback, useContext, useState, type ReactNode } from "react";

type TipoAviso = "exito" | "error" | "info" | "warn";
interface Aviso { id: number; tipo: TipoAviso; texto: string }

const Contexto = createContext<(tipo: TipoAviso, texto: string) => void>(() => undefined);

/** Notificaciones breves tras guardar, eliminar o cambiar estados. */
export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);
  const avisar = useCallback((tipo: TipoAviso, texto: string) => {
    const id = Date.now() + Math.random();
    setAvisos((lista) => [...lista.slice(-2), { id, tipo, texto }]);
    setTimeout(() => setAvisos((lista) => lista.filter((a) => a.id !== id)), tipo === "error" ? 7000 : 4000);
  }, []);

  return (
    <Contexto.Provider value={avisar}>
      {children}
      <div className="avisos-flotantes" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso ${a.tipo}`} role={a.tipo === "error" ? "alert" : "status"}>
            <span>{a.texto}</span>
            <button type="button" className="cerrar-aviso" aria-label="Cerrar aviso"
              onClick={() => setAvisos((lista) => lista.filter((x) => x.id !== a.id))}>×</button>
          </div>
        ))}
      </div>
    </Contexto.Provider>
  );
}

export const useAvisos = () => useContext(Contexto);
