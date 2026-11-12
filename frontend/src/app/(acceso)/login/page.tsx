"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api";
import { abrirSesion, clienteAuth, mensajeError, prepararCsrf, traducirErrorNeon } from "@/lib/auth";

type Mensaje = { tipo: "error" | "info" | "exito"; texto: string } | null;

export default function PaginaLogin() {
  const router = useRouter();
  const [listo, setListo] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<Mensaje>(null);

  useEffect(() => {
    let activo = true;
    (async () => {
      try {
        await prepararCsrf();
        const { config, neon } = await clienteAuth();
        if (!config.seguridad || !neon) { router.replace("/panel/"); return; }
        const parametros = new URLSearchParams(window.location.search);
        if (parametros.has("salir")) {
          await neon.signOut().catch(() => undefined);
          window.history.replaceState(null, "", "/login/");
          if (activo) setMensaje({ tipo: "exito", texto: "Sesión cerrada." });
        } else {
          // Si ya hay sesion en Neon Auth se reutiliza sin pedir la contraseña otra vez.
          const actual = await neon.getSession().catch(() => null);
          if (actual?.data?.user) {
            try { await abrirSesion(neon); router.replace("/panel/"); return; } catch (e) {
              if (activo) setMensaje({ tipo: e instanceof ApiError && e.estado === 403 ? "info" : "error", texto: mensajeError(e, "No fue posible iniciar sesión.") });
            }
          }
        }
      } catch {
        if (activo) setMensaje({ tipo: "error", texto: "No fue posible contactar el servicio de autenticación. Intente más tarde." });
      }
      if (activo) setListo(true);
    })();
    return () => { activo = false; };
  }, [router]);

  async function ingresar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const datos = new FormData(e.currentTarget);
    const correo = String(datos.get("correo") ?? "").trim();
    const clave = String(datos.get("clave") ?? "");
    if (!correo || !clave) { setMensaje({ tipo: "error", texto: "Ingrese su correo y su contraseña." }); return; }
    setEnviando(true);
    setMensaje(null);
    try {
      const { neon } = await clienteAuth();
      if (!neon) { router.replace("/panel/"); return; }
      const { error } = await neon.signIn.email({ email: correo, password: clave });
      if (error) throw new Error(traducirErrorNeon(error, "Correo o contraseña incorrectos."));
      await abrirSesion(neon);
      router.replace("/panel/");
    } catch (error) {
      setMensaje({ tipo: error instanceof ApiError && error.estado === 403 ? "info" : "error", texto: mensajeError(error, "No fue posible iniciar sesión.") });
      setEnviando(false);
    }
  }

  return (
    <section className="tarjeta-acceso" aria-labelledby="titulo-login">
      <h1 id="titulo-login">Iniciar sesión</h1>
      <p className="subtitulo">Ingrese con su cuenta para acceder al centro de control.</p>
      {mensaje ? <div className={`aviso ${mensaje.tipo}`} role={mensaje.tipo === "error" ? "alert" : "status"}>{mensaje.texto}</div> : null}
      <form className="formulario-acceso" noValidate onSubmit={ingresar}>
        <div className="campo">
          <label htmlFor="correo">Correo</label>
          <input id="correo" name="correo" type="email" autoComplete="email" required maxLength={160} autoFocus />
        </div>
        <div className="campo">
          <label htmlFor="clave">Contraseña</label>
          <input id="clave" name="clave" type="password" autoComplete="current-password" required maxLength={128} />
        </div>
        <button type="submit" className="boton primario ancho-total" disabled={!listo || enviando}>
          {enviando ? "Ingresando…" : listo ? "Iniciar sesión" : "Preparando…"}
        </button>
      </form>
      <p className="pie-acceso">¿No tiene cuenta? <Link href="/registro/">Crear una cuenta</Link></p>
    </section>
  );
}
