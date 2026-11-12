'use strict';

// Sesion del panel: agrega el token CSRF a las peticiones que modifican datos,
// redirige al login si la sesion expira y expone el usuario actual con su rol.
(function () {
    const fetchOriginal = window.fetch.bind(window);
    const METODOS_SEGUROS = ['GET', 'HEAD', 'OPTIONS'];
    const NOMBRES_ROL = { ADMIN: 'Administrador', OPERADOR: 'Operador', CONSULTA: 'Consulta' };

    const csrf = () => decodeURIComponent((document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/) || [])[1] || '');

    window.fetch = async (entrada, opciones = {}) => {
        const metodo = String(opciones.method || 'GET').toUpperCase();
        const cabeceras = new Headers(opciones.headers || {});
        if (!cabeceras.has('Accept')) cabeceras.set('Accept', 'application/json');
        if (!METODOS_SEGUROS.includes(metodo)) cabeceras.set('X-XSRF-TOKEN', csrf());
        const respuesta = await fetchOriginal(entrada, { ...opciones, headers: cabeceras, credentials: 'same-origin' });
        if (respuesta.status === 401) location.replace('/login.html');
        return respuesta;
    };

    async function salir() {
        try { await fetch('/auth/logout', { method: 'POST' }); } catch (e) { /* se cierra igual en Neon Auth */ }
        location.replace('/login.html?salir=1');
    }

    function pintarUsuario(usuario) {
        const iniciales = (usuario.nombre || usuario.correo || '?').split(/[\s@._-]+/).filter(Boolean).slice(0, 2)
            .map(p => p[0].toUpperCase()).join('');
        const avatar = document.getElementById('usuario-avatar');
        const nombre = document.getElementById('usuario-nombre');
        const rol = document.getElementById('usuario-rol');
        const boton = document.getElementById('boton-salir');
        if (avatar) avatar.textContent = iniciales || 'US';
        if (nombre) { nombre.textContent = usuario.nombre || usuario.correo; nombre.title = usuario.correo; }
        if (rol) rol.textContent = NOMBRES_ROL[usuario.rol] || usuario.rol;
        if (boton) boton.addEventListener('click', salir);
        document.body.dataset.rol = usuario.rol;
    }

    const listo = fetch('/auth/me')
        .then(r => r.ok ? r.json() : Promise.reject(new Error('Sin sesión')))
        .then(usuario => { pintarUsuario(usuario); return usuario; });

    window.SpaceTrackSesion = {
        listo,
        puedeEscribir: rol => rol === 'ADMIN' || rol === 'OPERADOR',
        esAdmin: rol => rol === 'ADMIN',
        salir
    };
})();
