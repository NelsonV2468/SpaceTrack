"use client";

import { useEffect, useRef, type ReactNode } from "react";

export function Modal({ titulo, angosto, alCerrar, children }: {
  titulo: string; angosto?: boolean; alCerrar: () => void; children: ReactNode;
}) {
  const caja = useRef<HTMLDivElement>(null);
  const cerrar = useRef(alCerrar);
  cerrar.current = alCerrar;

  useEffect(() => {
    const anterior = document.activeElement as HTMLElement | null;
    caja.current?.querySelector<HTMLElement>("input:not([type=hidden]):not([type=checkbox]), select, textarea")?.focus();
    const tecla = (e: KeyboardEvent) => { if (e.key === "Escape") cerrar.current(); };
    document.addEventListener("keydown", tecla);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", tecla);
      document.body.style.overflow = "";
      anterior?.focus?.();
    };
  }, []);

  return (
    <div className="capa" data-modal-abierto onMouseDown={(e) => { if (e.target === e.currentTarget) cerrar.current(); }}>
      <div ref={caja} className={`modal${angosto ? " angosto" : ""}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-cabecera">
          <h2>{titulo}</h2>
          <button type="button" className="cerrar" aria-label="Cerrar" onClick={() => cerrar.current()}>×</button>
        </div>
        <div className="modal-cuerpo">{children}</div>
      </div>
    </div>
  );
}
