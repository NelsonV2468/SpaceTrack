"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api";
import { abrirSesion, clienteAuth, mensajeError, prepararCsrf, traducirErrorNeon } from "@/lib/auth";

const MINIMO_CLAVE = 8;

export default function PaginaRegistro() {
  const router = useRouter();
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [pendiente, setPendiente] = useState(false);

  useEffect(() => {
    void prepararCsrf();
    clienteAuth().then(({ config }) => { if (!config.seguridad) router.replace("/panel/"); }).catch(() => undefined);
  }, [router]);

  function validar(nombre: string, correo: string, clave: string, confirmacion: string) {
    const e: Record<string, string> = {};
    if (!nombre) e.nombre = "Ingrese su nombre.";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) e.correo = "Ingrese un correo válido.";
    if (clave.length < MINIMO_CLAVE) e.clave = `La contraseña debe tener al menos ${MINIMO_CLAVE} caracteres.`;
    if (clave !== confirmacion) e.confirmacion = "Las contraseñas no coinciden.";
    return e;
  }

  async function registrar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const datos = new FormData(evento.currentTarget);
    const nombre = String(datos.get("nombre") ?? "").trim();
    const correo = String(datos.get("correo") ?? "").trim().toLowerCase();
    const clave = String(datos.get("clave") ?? "");
    const confirmacion = String(datos.get("confirmacion") ?? "");
    const validacion = validar(nombre, correo, clave, confirmacion);
    setErrores(validacion);
    setError(null);
    if (Object.keys(validacion).length) return;

    setEnviando(true);
    try {
      const { neon } = await clienteAuth();
      if (!neon) { router.replace("/panel/"); return; }
      const { error: fallo } = await neon.signUp.email({ name: nombre, email: correo, password: clave });
      if (fallo) throw new Error(traducirErrorNeon(fallo, "No fue posible crear la cuenta."));
      try {
        // Correos configurados como administradores entran de inmediato.
        await abrirSesion(neon);
        router.replace("/panel/");
      } catch (e) {
        if (e instanceof ApiError && e.estado === 403) {
          await neon.signOut().catch(() => undefined);
          setPendiente(true);
        } else throw e;
      }
    } catch (e) {
      setError(mensajeError(e, "No fue posible crear la cuenta."));
    } finally {
      setEnviando(false);
    }
  }

  if (pendiente) {
    return (
      <section className="tarjeta-acceso" aria-labelledby="titulo-registro">
        <h1 id="titulo-registro">Cuenta creada</h1>
        <div className="aviso exito" role="status">
          Su cuenta quedó registrada y está pendiente de aprobación. Un administrador debe asignarle un rol antes de que pueda ingresar.
        </div>
        <Link className="boton primario ancho-total" href="/login/">Ir a iniciar sesión</Link>
      </section>
    );
  }

  return (
    <section className="tarjeta-acceso" aria-labelledby="titulo-registro">
      <h1 id="titulo-registro">Crear cuenta</h1>
      <p className="subtitulo">Las cuentas nuevas quedan pendientes hasta que un administrador les asigne un rol.</p>
      {error ? <div className="aviso error" role="alert">{error}</div> : null}
      <form className="formulario-acceso" noValidate onSubmit={registrar}>
        <CampoAcceso id="nombre" etiqueta="Nombre completo" autoComplete="name" maxLength={120} error={errores.nombre} autoFocus />
        <CampoAcceso id="correo" etiqueta="Correo" tipo="email" autoComplete="email" maxLength={160} error={errores.correo} />
        <CampoAcceso id="clave" etiqueta="Contraseña" tipo="password" autoComplete="new-password" maxLength={128}
          ayuda={`Mínimo ${MINIMO_CLAVE} caracteres.`} error={errores.clave} />
        <CampoAcceso id="confirmacion" etiqueta="Confirmar contraseña" tipo="password" autoComplete="new-password" maxLength={128} error={errores.confirmacion} />
        <button type="submit" className="boton primario ancho-total" disabled={enviando}>{enviando ? "Creando cuenta…" : "Crear cuenta"}</button>
      </form>
      <p className="pie-acceso">¿Ya tiene cuenta? <Link href="/login/">Iniciar sesión</Link></p>
    </section>
  );
}

function CampoAcceso({ id, etiqueta, tipo = "text", autoComplete, maxLength, ayuda, error, autoFocus }: {
  id: string; etiqueta: string; tipo?: string; autoComplete: string; maxLength: number; ayuda?: string; error?: string; autoFocus?: boolean;
}) {
  return (
    <div className={`campo${error ? " con-error" : ""}`}>
      <label htmlFor={id}>{etiqueta}</label>
      <input id={id} name={id} type={tipo} autoComplete={autoComplete} maxLength={maxLength} required autoFocus={autoFocus}
        aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} />
      {ayuda && !error ? <span className="ayuda">{ayuda}</span> : null}
      {error ? <small id={`${id}-error`} role="alert">{error}</small> : null}
    </div>
  );
}
