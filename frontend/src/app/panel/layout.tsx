"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ProveedorAvisos } from "@/components/Avisos";
import { Icono } from "@/components/Icono";
import { api, configurarExpiracion } from "@/lib/api";
import { MENU, itemActivo } from "@/lib/navegacion";
import { ContextoSesion, NOMBRE_ROL, type Permisos } from "@/lib/sesion";
import type { UsuarioSesion } from "@/lib/tipos";

export default function LayoutPanel({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const ruta = usePathname();
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  const [menuAbierto, setMenuAbierto] = useState(false);

  useEffect(() => {
    configurarExpiracion(() => router.replace("/login/"));
    api<UsuarioSesion>("GET", "/auth/me").then(setUsuario).catch(() => router.replace("/login/"));
  }, [router]);

  // Cierra el menu movil al navegar.
  useEffect(() => { setMenuAbierto(false); }, [ruta]);

  const activo = itemActivo(ruta);
  useEffect(() => { document.title = `${activo?.texto ?? "Panel"} | SpaceTrack`; }, [activo]);

  const salir = useCallback(async () => {
    await api("POST", "/auth/logout").catch(() => undefined);
    window.location.replace("/login/?salir=1");
  }, []);

  const permisos = useMemo<Permisos | null>(() => usuario && {
    usuario,
    puedeEscribir: usuario.rol === "ADMIN" || usuario.rol === "OPERADOR",
    esAdmin: usuario.rol === "ADMIN",
    salir,
  }, [usuario, salir]);

  if (!permisos) return <div className="pantalla-carga" aria-busy="true">Cargando SpaceTrack…</div>;

  const iniciales = (usuario!.nombre || usuario!.correo).split(/[\s@._-]+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join("");

  return (
    <ContextoSesion.Provider value={permisos}>
      <ProveedorAvisos>
        <div className={`app${menuAbierto ? " menu-abierto" : ""}`}>
          <aside className="lateral" aria-label="Navegación principal">
            <div className="marca-app">
              <span className="isotipo" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z" /><circle cx="12" cy="9.5" r="2.5" />
                </svg>
              </span>
              <div><strong>SpaceTrack</strong><span>City Urban Bogotá</span></div>
              <button className="cerrar-menu" aria-label="Cerrar menú" onClick={() => setMenuAbierto(false)}><Icono nombre="cerrar" /></button>
            </div>
            <nav className="menu">
              {MENU.map(({ grupo, items }) => {
                const visibles = items.filter((i) => !i.soloAdmin || permisos.esAdmin);
                if (!visibles.length) return null;
                return (
                  <div key={grupo}>
                    <p className="grupo">{grupo}</p>
                    {visibles.map((i) => (
                      <Link key={i.href} href={i.href} className={activo?.href === i.href ? "activo" : undefined}
                        aria-current={activo?.href === i.href ? "page" : undefined}>
                        <Icono nombre={i.icono} /><span className="texto">{i.texto}</span>
                      </Link>
                    ))}
                  </div>
                );
              })}
            </nav>
          </aside>
          <div className="velo" onClick={() => setMenuAbierto(false)} aria-hidden="true" />

          <div className="principal">
            <header className="barra-superior">
              <div className="barra-izq">
                <button className="boton-menu" aria-label="Abrir menú" aria-expanded={menuAbierto} onClick={() => setMenuAbierto(true)}>
                  <Icono nombre="menu" />
                </button>
                <span className="titulo-barra">{activo?.texto ?? "SpaceTrack"}</span>
              </div>
              <div className="usuario">
                <span className="avatar" aria-hidden="true">{iniciales || "US"}</span>
                <span className="usuario-datos">
                  <span className="usuario-nombre" title={usuario!.correo}>{usuario!.nombre || usuario!.correo}</span>
                  <span className="sec">{NOMBRE_ROL[usuario!.rol]}</span>
                </span>
                <button className="boton secundario chico" onClick={salir} title="Cerrar sesión">
                  <Icono nombre="salir" /><span className="texto-salir">Salir</span>
                </button>
              </div>
            </header>
            <main className="contenido">{children}</main>
          </div>
        </div>
      </ProveedorAvisos>
    </ContextoSesion.Provider>
  );
}
