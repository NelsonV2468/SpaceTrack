import { createAuthClient } from "@neon/auth";

// Flujo: Neon Auth valida correo y contrasena -> se obtiene un JWT de 15 min ->
// POST /auth/session lo cambia por una cookie de sesion HttpOnly de SpaceTrack.

const form = document.querySelector("form");
const aviso = document.querySelector("[data-error]");
const botones = form.querySelectorAll("button");

const csrf = () => decodeURIComponent((document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/) || [])[1] || "");

function mostrar(texto, tipo = "error") {
  aviso.textContent = texto;
  aviso.dataset.tipo = tipo;
}

function ocupado(activo) {
  botones.forEach(b => { b.disabled = activo; });
}

async function abrirSesionSpaceTrack(auth) {
  const { data, error } = await auth.token();
  if (error || !data?.token) throw new Error("No fue posible obtener el token de Neon Auth");
  const respuesta = await fetch("/auth/session", {
    method: "POST",
    credentials: "same-origin",
    headers: { Authorization: `Bearer ${data.token}`, "X-XSRF-TOKEN": csrf(), Accept: "application/json" }
  });
  const cuerpo = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    const fallo = new Error(cuerpo.mensaje || "No fue posible iniciar sesión");
    fallo.estado = respuesta.status;
    throw fallo;
  }
  location.replace("/panel.html");
}

async function iniciar() {
  const config = await fetch("/auth/config", { headers: { Accept: "application/json" } }).then(r => r.json());
  if (!config.seguridad) {
    // Desarrollo local sin login.
    location.replace("/panel.html");
    return;
  }
  const auth = createAuthClient(config.baseUrl);
  const parametros = new URLSearchParams(location.search);

  if (parametros.has("salir")) {
    await auth.signOut().catch(() => {});
    history.replaceState(null, "", "/login.html");
    mostrar("Sesión cerrada.", "ok");
  } else {
    // Si ya hay sesion en Neon Auth, se reutiliza sin pedir la contrasena otra vez.
    const actual = await auth.getSession().catch(() => null);
    if (actual?.data?.user) {
      try { await abrirSesionSpaceTrack(auth); return; } catch (e) { mostrar(e.message); }
    }
  }

  form.addEventListener("submit", async evento => {
    evento.preventDefault();
    mostrar("");
    ocupado(true);
    try {
      const valores = new FormData(form);
      const { error } = await auth.signIn.email({ email: valores.get("email"), password: valores.get("password") });
      if (error) throw new Error(traducir(error, "Correo o contraseña incorrectos"));
      await abrirSesionSpaceTrack(auth);
    } catch (e) {
      mostrar(e.message, e.estado === 403 ? "info" : "error");
    } finally {
      ocupado(false);
    }
  });

  document.querySelector("[data-signup]").addEventListener("click", async () => {
    if (!form.reportValidity()) return;
    mostrar("");
    ocupado(true);
    try {
      const valores = new FormData(form);
      const correo = String(valores.get("email"));
      const { error } = await auth.signUp.email({
        name: String(valores.get("name") || "").trim() || correo.split("@")[0],
        email: correo,
        password: valores.get("password")
      });
      if (error) throw new Error(traducir(error, "No fue posible crear la cuenta"));
      // Registra la cuenta en SpaceTrack; normalmente queda pendiente de aprobacion.
      await abrirSesionSpaceTrack(auth);
    } catch (e) {
      mostrar(e.message, e.estado === 403 ? "info" : "error");
    } finally {
      ocupado(false);
    }
  });
}

function traducir(error, porDefecto) {
  const codigo = error?.code || "";
  const mensajes = {
    INVALID_EMAIL_OR_PASSWORD: "Correo o contraseña incorrectos",
    USER_ALREADY_EXISTS: "Ya existe una cuenta con ese correo",
    USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL: "Ya existe una cuenta con ese correo",
    PASSWORD_TOO_SHORT: "La contraseña es demasiado corta",
    EMAIL_NOT_VERIFIED: "Debe verificar su correo antes de ingresar",
    TOO_MANY_REQUESTS: "Demasiados intentos. Espere un momento"
  };
  return mensajes[codigo] || error?.message || porDefecto;
}

iniciar().catch(() => mostrar("No fue posible contactar el servicio de autenticación. Intente más tarde."));
