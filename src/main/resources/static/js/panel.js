// ------------------------------------------------------------------
// SpaceTrack - Panel de operacion (cliente de la API REST local).
// ------------------------------------------------------------------
const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'];
const INICIAL_DIA = { LUNES: 'L', MARTES: 'M', MIERCOLES: 'X', JUEVES: 'J', VIERNES: 'V', SABADO: 'S', DOMINGO: 'D' };
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const TIPOS_VEHICULO = ['Camion', 'Camioneta', 'Furgon', 'Motocicleta'];
const REFRESCO_MAPA_MS = 5000;

const ICONOS = {
    tablero: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z',
    monitoreo: 'M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21zM12 12.2a2.7 2.7 0 1 0 0-5.4 2.7 2.7 0 0 0 0 5.4z',
    clientes: 'M4 21V5l8-3 8 3v16M9 21v-5h6v5M8 8h2M14 8h2M8 12h2M14 12h2',
    vehiculos: 'M2 16V6h12v10M14 9h4l4 4v3h-8M6.5 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM17.5 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    conductores: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0',
    operaciones: 'M7 3h7l5 5v13H7zM14 3v5h5M10 13h6M10 17h6',
    necesidades: 'M9 4h6v3H9zM6 6h3M15 6h3v15H6V6M9 12l2 2 4-4',
    zonas: 'M9 3 3 6v15l6-3 6 3 6-3V3l-6 3-6-3zM9 3v15M15 6v15',
    rutas: 'M6 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM18 9a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM6 15V9a3 3 0 0 1 3-3h7M18 9v6a3 3 0 0 1-3 3H8',
    entregas: 'M21 8 12 3 3 8v8l9 5 9-5zM3 8l9 5 9-5M12 13v8',
    buscar: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM21 21l-5-5',
    mas: 'M12 5v14M5 12h14',
    persona: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21a8 8 0 0 1 16 0'
};

function icono(nombre) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', 'currentColor');
    svg.setAttribute('stroke-width', '1.8');
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', ICONOS[nombre] || '');
    svg.append(path);
    return svg;
}

// ------------------------------------------------------------------
// Utilidades de presentacion
// ------------------------------------------------------------------
function el(etiquetaHtml, atributos = {}, hijos = []) {
    const nodo = document.createElement(etiquetaHtml);
    Object.entries(atributos).forEach(([clave, valor]) => {
        if (clave === 'texto') nodo.textContent = valor;
        else if (clave.startsWith('on')) nodo.addEventListener(clave.slice(2), valor);
        else if (valor !== false && valor !== undefined && valor !== null) nodo.setAttribute(clave, valor === true ? '' : valor);
    });
    [].concat(hijos).forEach(hijo => hijo !== null && hijo !== undefined && hijo !== false
        && nodo.append(typeof hijo === 'string' || typeof hijo === 'number' ? String(hijo) : hijo));
    return nodo;
}
function doble(principal, secundario) {
    return el('div', {}, [el('strong', { texto: principal ?? '' }), secundario ? el('span', { class: 'sec', texto: secundario }) : null]);
}
function etiqueta(texto, tono) { return el('span', { class: 'etiqueta ' + tono, texto }); }
function activo(valor, femenino) { return etiqueta(valor ? (femenino ? 'Activa' : 'Activo') : (femenino ? 'Inactiva' : 'Inactivo'), valor ? 'ok' : 'neutral'); }
function siNo(valor) { return etiqueta(valor ? 'Sí' : 'No', valor ? 'info' : 'neutral'); }
function fecha(valor) {
    if (!valor) return '—';
    const [a, m, d] = valor.slice(0, 10).split('-');
    return d + ' ' + MESES[Number(m) - 1] + ' ' + a;
}
function fechaHora(valor) { return valor ? fecha(valor) + ', ' + valor.slice(11, 16) : '—'; }
function hora(valor) { return valor ? valor.slice(0, 5) : ''; }
function miles(valor) { return Number(valor ?? 0).toLocaleString('es-CO'); }
function porcentaje(parte, total) { return total ? Math.round(parte * 100 / total) : 0; }
function hoy(dias) {
    const f = new Date();
    f.setDate(f.getDate() + dias);
    return f.getFullYear() + '-' + String(f.getMonth() + 1).padStart(2, '0') + '-' + String(f.getDate()).padStart(2, '0');
}
function diasSemana(lista) {
    return el('span', { class: 'dias-semana', title: lista.join(', ') }, DIAS.map(d => el('span', { class: lista.includes(d) ? 'on' : '', texto: INICIAL_DIA[d] })));
}

const ESTADOS_RUTA = { PENDIENTE: ['Pendiente', 'neutral'], EN_CURSO: ['En curso', 'info'], FINALIZADA: ['Finalizada', 'ok'] };
function estadoRuta(estado) { const [t, c] = ESTADOS_RUTA[estado] || [estado, 'neutral']; return etiqueta(t, c); }
const ESTADOS_OPERACION = { ACTIVA: 'ok', PAUSADA: 'warn', FINALIZADA: 'neutral' };

function barraEntregas(r, ancho) {
    const p = r.numeroEntregasProgramadas || 0;
    return el('div', { class: 'progreso', style: ancho ? 'min-width:' + ancho + 'px' : null }, [
        el('div', { class: 'barra' }, [
            el('i', { class: 'ok', style: 'width:' + porcentaje(r.entregasRealizadas, p) + '%' }),
            el('i', { class: 'danger', style: 'width:' + porcentaje(r.entregasNoRealizadas, p) + '%' })
        ]),
        el('span', { class: 'cifra num', texto: r.entregasRealizadas + ' / ' + p })
    ]);
}

function nombreConductor(c) { return c.nombres + ' ' + c.apellidos; }
function licenciaVigente(c) { return c.fechaVencimientoLicencia >= hoy(0); }

// ------------------------------------------------------------------
// Configuracion de los modulos de registro
// ------------------------------------------------------------------
const MODULOS = {
    clientes: {
        grupo: 'Gestión', titulo: 'Clientes', singular: 'cliente', api: '/api/clientes', icono: 'clientes',
        descripcion: 'Empresas contratantes del servicio de distribución.',
        columnas: [
            ['Empresa', c => doble(c.razonSocial, 'NIT ' + c.nit)],
            ['Contacto', c => doble(c.nombreContacto, c.correoContacto + ' · ' + c.telefonoContacto)],
            ['Dirección', c => c.direccion],
            ['Estado', c => activo(c.activo)]
        ],
        campos: [
            { nombre: 'razonSocial', etiqueta: 'Razón social', ancho: true },
            { nombre: 'nit', etiqueta: 'NIT', ayuda: 'Ej.: 900123456-7' },
            { nombre: 'nombreContacto', etiqueta: 'Nombre de contacto' },
            { nombre: 'correoContacto', etiqueta: 'Correo de contacto', tipo: 'email' },
            { nombre: 'telefonoContacto', etiqueta: 'Teléfono de contacto' },
            { nombre: 'direccion', etiqueta: 'Dirección', ancho: true },
            { nombre: 'activo', etiqueta: 'Cliente activo', tipo: 'check', valor: true }
        ]
    },
    vehiculos: {
        grupo: 'Gestión', titulo: 'Vehículos', singular: 'vehículo', api: '/api/vehiculos', icono: 'vehiculos',
        descripcion: 'Flota disponible para asignar a las rutas.',
        columnas: [
            ['Placa', v => el('strong', { class: 'num', texto: v.placa })],
            ['Vehículo', v => doble(v.tipoVehiculo, v.marca + ' ' + v.modelo + ' · ' + v.anioModelo)],
            ['Capacidad', v => miles(v.capacidadCargaKg) + ' kg'],
            ['Cadena de frío', v => siNo(v.requiereCadenaFrio)],
            ['Estado', v => activo(v.activo)]
        ],
        campos: [
            { nombre: 'placa', etiqueta: 'Placa', ayuda: 'Ej.: ABC123' },
            { nombre: 'tipoVehiculo', etiqueta: 'Tipo de vehículo', tipo: 'select', opciones: TIPOS_VEHICULO },
            { nombre: 'marca', etiqueta: 'Marca' },
            { nombre: 'modelo', etiqueta: 'Modelo' },
            { nombre: 'anioModelo', etiqueta: 'Año del modelo', tipo: 'number', valor: new Date().getFullYear() },
            { nombre: 'capacidadCargaKg', etiqueta: 'Capacidad de carga (kg)', tipo: 'number' },
            { nombre: 'requiereCadenaFrio', etiqueta: 'Cuenta con cadena de frío', tipo: 'check' },
            { nombre: 'activo', etiqueta: 'Vehículo activo', tipo: 'check', valor: true }
        ]
    },
    conductores: {
        grupo: 'Gestión', titulo: 'Conductores', singular: 'conductor', api: '/api/conductores', icono: 'conductores',
        descripcion: 'Personal habilitado para operar la flota.',
        columnas: [
            ['Conductor', c => doble(nombreConductor(c), c.tipoDocumento + ' ' + c.numeroDocumento)],
            ['Contacto', c => doble(c.telefono, c.correo)],
            ['Licencia', c => doble(c.numeroLicencia, 'Vence ' + fecha(c.fechaVencimientoLicencia))],
            ['Habilitación', c => !c.activo ? etiqueta('Inactivo', 'neutral') : licenciaVigente(c) ? etiqueta('Habilitado', 'ok') : etiqueta('Licencia vencida', 'danger')]
        ],
        campos: [
            { nombre: 'tipoDocumento', etiqueta: 'Tipo de documento', tipo: 'select', opciones: ['CC', 'CE', 'PASAPORTE'] },
            { nombre: 'numeroDocumento', etiqueta: 'Número de documento' },
            { nombre: 'nombres', etiqueta: 'Nombres' },
            { nombre: 'apellidos', etiqueta: 'Apellidos' },
            { nombre: 'telefono', etiqueta: 'Teléfono' },
            { nombre: 'correo', etiqueta: 'Correo', tipo: 'email' },
            { nombre: 'numeroLicencia', etiqueta: 'Número de licencia' },
            { nombre: 'fechaVencimientoLicencia', etiqueta: 'Vencimiento de la licencia', tipo: 'date' },
            { nombre: 'activo', etiqueta: 'Conductor activo', tipo: 'check', valor: true }
        ]
    },
    operaciones: {
        grupo: 'Gestión', titulo: 'Operaciones', singular: 'operación', api: '/api/operaciones', icono: 'operaciones',
        descripcion: 'Contratos de distribución suscritos con cada cliente.',
        columnas: [
            ['Contrato', o => doble(o.codigoContrato, o.nombreOperacion)],
            ['Cliente', o => o.razonSocialCliente],
            ['Vigencia', o => doble(fecha(o.fechaInicio) + ' – ' + fecha(o.fechaFin), o.diasOperacion)],
            ['Alcance', o => doble(o.numeroVehiculosRequeridos + ' vehículos', miles(o.numeroEntregasEsperadas) + ' entregas esperadas')],
            ['Estado', o => etiqueta(o.estado.charAt(0) + o.estado.slice(1).toLowerCase(), ESTADOS_OPERACION[o.estado] || 'neutral')]
        ],
        campos: [
            { nombre: 'codigoContrato', etiqueta: 'Código de contrato', ayuda: 'Ej.: CT-2026-001' },
            { nombre: 'clienteId', etiqueta: 'Cliente', tipo: 'relacion', fuente: 'clientes', mostrar: c => c.razonSocial },
            { nombre: 'nombreOperacion', etiqueta: 'Nombre de la operación', ancho: true },
            { nombre: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'date', valor: hoy(0) },
            { nombre: 'fechaFin', etiqueta: 'Fecha de finalización', tipo: 'date', valor: hoy(90) },
            { nombre: 'diasOperacion', etiqueta: 'Días de operación (resumen)', valor: 'Lunes a sábado' },
            { nombre: 'estado', etiqueta: 'Estado', tipo: 'select', opciones: ['ACTIVA', 'PAUSADA', 'FINALIZADA'] },
            { nombre: 'numeroVehiculosRequeridos', etiqueta: 'Vehículos requeridos', tipo: 'number', valor: 1 },
            { nombre: 'numeroEntregasEsperadas', etiqueta: 'Entregas esperadas', tipo: 'number', valor: 1 },
            { nombre: 'requiereCadenaFrio', etiqueta: 'Requiere cadena de frío', tipo: 'check' }
        ]
    },
    necesidades: {
        grupo: 'Planeación', titulo: 'Necesidades del cliente', menu: 'Necesidades', singular: 'necesidad', api: '/api/necesidades', icono: 'necesidades',
        descripcion: 'Requerimiento de cada contrato: vehículos, días y franja horaria de operación.',
        columnas: [
            ['Contrato', n => doble(n.codigoContrato, n.razonSocialCliente)],
            ['Vehículos requeridos', n => doble(n.cantidadVehiculos + ' × ' + n.tipoVehiculo, 'Mín. ' + miles(n.capacidadMinimaKg) + ' kg' + (n.requiereCadenaFrio ? ' · cadena de frío' : ''))],
            ['Días de operación', n => diasSemana(n.diasOperacion)],
            ['Horario', n => el('span', { class: 'num', texto: hora(n.horaInicio) + ' – ' + hora(n.horaFin) })],
            ['Cobertura de flota', n => etiqueta(n.vehiculosDisponibles + ' de ' + n.cantidadVehiculos + ' disponibles', n.flotaSuficiente ? 'ok' : 'warn')]
        ],
        campos: [
            { nombre: 'operacionId', etiqueta: 'Operación / contrato', tipo: 'relacion', fuente: 'operaciones', mostrar: o => o.codigoContrato + ' — ' + o.razonSocialCliente, ancho: true },
            { nombre: 'tipoVehiculo', etiqueta: 'Tipo de vehículo', tipo: 'select', opciones: TIPOS_VEHICULO },
            { nombre: 'cantidadVehiculos', etiqueta: 'Cantidad de vehículos', tipo: 'number', valor: 1 },
            { nombre: 'capacidadMinimaKg', etiqueta: 'Capacidad mínima (kg)', tipo: 'number' },
            { nombre: 'requiereCadenaFrio', etiqueta: 'Requiere cadena de frío', tipo: 'check' },
            { nombre: 'diasOperacion', etiqueta: 'Días de operación', tipo: 'dias', ancho: true },
            { nombre: 'horaInicio', etiqueta: 'Hora de inicio', tipo: 'time', valor: '06:00' },
            { nombre: 'horaFin', etiqueta: 'Hora de finalización', tipo: 'time', valor: '14:00' },
            { nombre: 'observaciones', etiqueta: 'Observaciones', ancho: true, opcional: true, tipo: 'textarea' }
        ]
    },
    zonas: {
        grupo: 'Planeación', titulo: 'Zonas', singular: 'zona', femenino: true, api: '/api/zonas', icono: 'zonas',
        descripcion: 'Áreas de la ciudad cubiertas por las rutas. La localidad define la ubicación en el mapa.',
        columnas: [
            ['Zona', z => doble(z.nombre, z.codigo)],
            ['Localidad', z => z.localidad],
            ['Descripción', z => z.descripcion || '—'],
            ['Estado', z => activo(z.activa, true)]
        ],
        campos: [
            { nombre: 'codigo', etiqueta: 'Código', ayuda: 'Ej.: Z-NORTE' },
            { nombre: 'nombre', etiqueta: 'Nombre' },
            { nombre: 'localidad', etiqueta: 'Localidad', ayuda: 'Ej.: Usaquén, Kennedy, Suba, Chapinero' },
            { nombre: 'descripcion', etiqueta: 'Descripción', opcional: true },
            { nombre: 'activa', etiqueta: 'Zona activa', tipo: 'check', valor: true }
        ]
    },
    rutas: {
        grupo: 'Planeación', titulo: 'Rutas', singular: 'ruta', femenino: true, api: '/api/rutas', icono: 'rutas',
        descripcion: 'Recorridos de distribución con sus recursos, entregas programadas y estado.',
        alClic: r => irDetalleRuta(r.id),
        columnas: [
            ['Ruta', r => doble(r.codigo, r.nombre)],
            ['Zona / contrato', r => doble(r.nombreZona, r.codigoContrato)],
            ['Vehículo y conductor', r => r.placaVehiculo ? doble(r.placaVehiculo, r.nombreConductor) : etiqueta('Sin asignar', 'warn')],
            ['Entregas realizadas', r => barraEntregas(r, 170)],
            ['Estado', r => el('div', {}, [estadoRuta(r.estado), el('span', { class: 'sec', texto: r.estado === 'EN_CURSO' ? 'Desde ' + fechaHora(r.fechaInicioReal) : r.estado === 'FINALIZADA' ? 'Cerrada ' + fechaHora(r.fechaFinReal) : '' })])]
        ],
        filtro: { parametro: 'estado', etiqueta: 'Estado', opciones: [['PENDIENTE', 'Pendiente'], ['EN_CURSO', 'En curso'], ['FINALIZADA', 'Finalizada']] },
        accionesFila: r => [el('button', { class: 'boton secundario chico', onclick: e => { e.stopPropagation(); irDetalleRuta(r.id); } }, 'Ver detalle')],
        campos: [
            { nombre: 'codigo', etiqueta: 'Código', ayuda: 'Ej.: R-001' },
            { nombre: 'nombre', etiqueta: 'Nombre' },
            { nombre: 'zonaId', etiqueta: 'Zona', tipo: 'relacion', fuente: 'zonas', filtrar: z => z.activa, mostrar: z => z.nombre + ' (' + z.localidad + ')' },
            { nombre: 'operacionId', etiqueta: 'Operación / contrato', tipo: 'relacion', fuente: 'operaciones', filtrar: o => o.estado !== 'FINALIZADA', mostrar: o => o.codigoContrato + ' — ' + o.razonSocialCliente },
            { nombre: 'puntoOrigen', etiqueta: 'Punto de origen', valor: 'CEDI Puente Aranda' },
            { nombre: 'puntoDestino', etiqueta: 'Punto de destino' },
            { nombre: 'distanciaEstimadaKm', etiqueta: 'Distancia estimada (km)', tipo: 'number' },
            { nombre: 'numeroEntregasProgramadas', etiqueta: 'Entregas programadas (esperadas en la ruta)', tipo: 'number', valor: 1 }
        ]
    },
    entregas: {
        grupo: 'Ejecución', titulo: 'Registro de entregas', menu: 'Entregas', singular: 'entrega', femenino: true, api: '/api/entregas', icono: 'entregas',
        descripcion: 'Entregas realizadas en campo frente a las programadas en cada ruta.',
        vista: () => pintarEntregas()
    }
};

const VISTAS_GENERALES = {
    tablero: { titulo: 'Tablero de control', menu: 'Tablero', icono: 'tablero' },
    monitoreo: { titulo: 'Monitoreo de flota', menu: 'Monitoreo en mapa', icono: 'monitoreo' }
};

// ------------------------------------------------------------------
// Estado y acceso a la API
// ------------------------------------------------------------------
const estado = { vista: 'tablero', rutaId: null, datos: {}, filtros: {}, busqueda: {}, mensaje: null, temporizador: null, seleccionMapa: null, filtroMapa: 'ACTIVAS' };
const $contenido = document.getElementById('contenido');

async function api(metodo, url, cuerpo) {
    let respuesta;
    try {
        respuesta = await fetch(url, {
            method: metodo,
            headers: cuerpo ? { 'Content-Type': 'application/json' } : {},
            body: cuerpo ? JSON.stringify(cuerpo) : undefined
        });
        marcarServidor(true);
    } catch (e) {
        marcarServidor(false);
        const error = new Error('No hay conexión con el servidor local. Verifique que SpaceTrack esté en ejecución.');
        error.campos = {};
        throw error;
    }
    if (respuesta.status === 204) return null;
    const datos = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
        const error = new Error(datos.mensaje || 'No se pudo completar la operación (' + respuesta.status + ')');
        error.campos = datos.errores || {};
        throw error;
    }
    return datos;
}

async function cargar(clave, url) {
    try {
        estado.datos[clave] = await api('GET', url || MODULOS[clave].api);
    } catch (e) {
        estado.datos[clave] = null;
    }
    return estado.datos[clave];
}
const cargarVarios = claves => Promise.all(claves.map(c => cargar(c)));

function marcarServidor(conectado) {
    const nodo = document.getElementById('estado-servidor');
    if (!nodo) return;
    nodo.replaceChildren(el('span', { class: 'punto' + (conectado ? '' : ' caido') }), conectado ? 'Servidor local conectado' : 'Servidor sin conexión');
}

// ------------------------------------------------------------------
// Navegacion
// ------------------------------------------------------------------
function pintarMenu() {
    const menu = document.getElementById('menu');
    menu.replaceChildren();
    menu.append(el('p', { class: 'grupo', texto: 'General' }));
    Object.entries(VISTAS_GENERALES).forEach(([clave, v]) => menu.append(botonMenu(clave, v.menu, v.icono, null)));
    let grupo = '';
    Object.entries(MODULOS).forEach(([clave, m]) => {
        if (m.grupo !== grupo) { grupo = m.grupo; menu.append(el('p', { class: 'grupo', texto: grupo })); }
        const lista = estado.datos[clave];
        menu.append(botonMenu(clave, m.menu || m.titulo, m.icono, Array.isArray(lista) && !estado.filtros[clave] ? lista.length : null));
    });
}
function botonMenu(clave, titulo, nombreIcono, cantidad) {
    const activoMenu = estado.vista === clave || (clave === 'rutas' && estado.vista === 'ruta');
    return el('button', { class: activoMenu ? 'activo' : '', onclick: () => ir(clave) },
        [icono(nombreIcono), el('span', { class: 'texto', texto: titulo }), cantidad === null ? null : el('span', { class: 'contador num', texto: cantidad })]);
}

function migas(grupo, titulo) {
    document.getElementById('migas').replaceChildren('SpaceTrack', el('span', { class: 'sep', texto: '/' }), grupo,
        el('span', { class: 'sep', texto: '/' }), el('strong', { texto: titulo }));
}

function detenerTemporizador() {
    if (estado.temporizador) clearInterval(estado.temporizador);
    estado.temporizador = null;
}

async function ir(vista, conservarMensaje) {
    detenerTemporizador();
    cerrarModal();
    estado.vista = vista;
    if (!conservarMensaje) estado.mensaje = null;
    // La vista actual queda en la URL para que F5 conserve la pantalla.
    history.replaceState(null, '', '#' + (vista === 'ruta' ? 'ruta/' + estado.rutaId : vista));
    $contenido.replaceChildren(el('div', { class: 'vacio', texto: 'Cargando…' }));
    if (vista === 'tablero') await pintarTablero();
    else if (vista === 'monitoreo') await pintarMonitoreo();
    else if (vista === 'ruta') await pintarDetalleRuta();
    else if (MODULOS[vista].vista) await MODULOS[vista].vista();
    else await pintarModulo(vista);
    pintarMenu();
    window.scrollTo(0, 0);
}

function irDetalleRuta(id) {
    estado.rutaId = id;
    return ir('ruta');
}

function encabezado(titulo, subtitulo, acciones = [], volver) {
    return el('header', { class: 'encabezado' }, [
        el('div', {}, [
            volver ? el('button', { class: 'volver', onclick: volver[1] }, '← ' + volver[0]) : null,
            el('h1', { texto: titulo }),
            subtitulo ? el('p', { class: 'subtitulo', texto: subtitulo }) : null
        ]),
        acciones.length ? el('div', { class: 'acciones' }, acciones) : null
    ]);
}

function avisoActual() {
    if (!estado.mensaje) return null;
    return el('div', { class: 'aviso ' + estado.mensaje.tipo, role: 'status', texto: estado.mensaje.texto });
}

function vacio(titulo, texto) { return el('div', { class: 'vacio' }, [el('strong', { texto: titulo }), texto]); }

// ------------------------------------------------------------------
// Tablero de control
// ------------------------------------------------------------------
async function pintarTablero() {
    migas('General', 'Tablero de control');
    await cargarVarios(['rutas', 'vehiculos', 'conductores', 'necesidades', 'operaciones', 'clientes', 'zonas', 'entregas']);
    const rutas = estado.datos.rutas || [];
    const vehiculos = estado.datos.vehiculos || [];
    const conductores = estado.datos.conductores || [];
    const necesidades = estado.datos.necesidades || [];
    const cuenta = e => rutas.filter(r => r.estado === e).length;
    const iniciadas = rutas.filter(r => r.estado !== 'PENDIENTE');
    const programadas = iniciadas.reduce((s, r) => s + r.numeroEntregasProgramadas, 0);
    const realizadas = iniciadas.reduce((s, r) => s + r.entregasRealizadas, 0);
    const noRealizadas = iniciadas.reduce((s, r) => s + r.entregasNoRealizadas, 0);
    const habilitados = conductores.filter(c => c.activo && licenciaVigente(c)).length;
    const sinRecursos = rutas.filter(r => r.estado === 'PENDIENTE' && !r.placaVehiculo).length;
    const necesidadesSinFlota = necesidades.filter(n => !n.flotaSuficiente).length;

    $contenido.replaceChildren(encabezado('Tablero de control', 'Resumen de la operación de distribución de City Urban Bogotá.', [
        el('button', { class: 'boton secundario', onclick: () => ir('monitoreo') }, [icono('monitoreo'), 'Ver mapa']),
        el('button', { class: 'boton primario', onclick: () => ir('rutas') }, [icono('rutas'), 'Gestionar rutas'])
    ]));

    $contenido.append(el('section', { class: 'kpis' }, [
        kpi('Rutas en curso', cuenta('EN_CURSO'), 'de ' + rutas.length + ' rutas registradas', '#1d5fbf'),
        kpi('Rutas pendientes', cuenta('PENDIENTE'), sinRecursos ? sinRecursos + ' sin vehículo o conductor' : 'Todas con recursos asignados', sinRecursos ? '#9a5b00' : '#55627a'),
        kpi('Rutas finalizadas', cuenta('FINALIZADA'), 'Cerradas en el sistema', '#0f7b4f'),
        kpi('Cumplimiento de entregas', porcentaje(realizadas, programadas) + '%', miles(realizadas) + ' de ' + miles(programadas) + ' programadas' + (noRealizadas ? ' · ' + noRealizadas + ' con novedad' : ''), '#0f7b4f'),
        kpi('Flota activa', vehiculos.filter(v => v.activo).length, 'de ' + vehiculos.length + ' vehículos', '#16355a'),
        kpi('Conductores habilitados', habilitados, 'de ' + conductores.length + ' registrados', '#16355a')
    ]));

    if (necesidadesSinFlota) {
        $contenido.append(el('div', { class: 'aviso warn' }, [
            el('span', { texto: necesidadesSinFlota + ' necesidad(es) de cliente no tienen suficientes vehículos compatibles en la flota. ' }),
            el('button', { class: 'enlace', onclick: () => ir('necesidades') }, 'Revisar necesidades')
        ]));
    }

    const enCurso = rutas.filter(r => r.estado !== 'FINALIZADA');
    const tabla = enCurso.length ? el('div', { class: 'tabla' }, el('table', {}, [
        el('thead', {}, el('tr', {}, ['Ruta', 'Zona', 'Vehículo / conductor', 'Entregas realizadas vs. programadas', 'Estado'].map(t => el('th', { texto: t })))),
        el('tbody', {}, enCurso.map(r => el('tr', { class: 'clic', onclick: () => irDetalleRuta(r.id) }, [
            el('td', {}, doble(r.codigo, r.nombre)),
            el('td', {}, doble(r.nombreZona, r.codigoContrato)),
            el('td', {}, r.placaVehiculo ? doble(r.placaVehiculo, r.nombreConductor) : etiqueta('Sin asignar', 'warn')),
            el('td', {}, barraEntregas(r, 220)),
            el('td', {}, estadoRuta(r.estado))
        ])))
    ])) : vacio('No hay rutas activas', 'Las rutas pendientes y en curso aparecerán aquí.');
    $contenido.append(el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Rutas activas' }), leyendaEntregas()]),
        tabla
    ]));

    const pasos = [
        ['Clientes y contratos', 'Registre la empresa y su operación.', 'clientes'],
        ['Necesidad del cliente', 'Vehículos, días y horario requeridos.', 'necesidades'],
        ['Zonas y rutas', 'Defina recorridos y entregas programadas.', 'rutas'],
        ['Asignación', 'Vehículo y conductor por ruta.', 'rutas'],
        ['Ejecución', 'Inicie la ruta y registre las entregas.', 'entregas']
    ];
    $contenido.append(el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, el('h2', { texto: 'Flujo de trabajo' })),
        el('div', { class: 'panel-cuerpo' }, el('ol', { class: 'proceso', style: 'margin:0;padding:0' }, pasos.map(([t, d, v]) =>
            el('li', {}, [el('button', { class: 'enlace', onclick: () => ir(v) }, el('strong', { texto: t })), el('span', { texto: d })]))))
    ]));
}

function kpi(titulo, valor, nota, acento) {
    return el('div', { class: 'kpi', style: '--acento:' + acento }, [
        el('div', { class: 'titulo', texto: titulo }),
        el('div', { class: 'valor num', texto: valor }),
        el('div', { class: 'nota', texto: nota })
    ]);
}

function leyendaEntregas() {
    return el('div', { class: 'leyenda' }, [
        el('span', {}, [el('i', { style: 'background:var(--ok)' }), 'Entregadas']),
        el('span', {}, [el('i', { style: 'background:#d9534a' }), 'No entregadas']),
        el('span', {}, [el('i', { style: 'background:var(--neutral-soft)' }), 'Pendientes'])
    ]);
}

// ------------------------------------------------------------------
// Vista generica de un modulo: tabla con busqueda + formulario modal
// ------------------------------------------------------------------
async function pintarModulo(clave) {
    const m = MODULOS[clave];
    migas(m.grupo, m.titulo);
    const valorFiltro = m.filtro ? estado.filtros[clave] : '';
    await cargar(clave, m.api + (valorFiltro ? '?' + m.filtro.parametro + '=' + encodeURIComponent(valorFiltro) : ''));

    $contenido.replaceChildren(encabezado(m.titulo, m.descripcion, [
        el('button', { class: 'boton primario', onclick: () => abrirFormulario(clave) }, [icono('mas'), 'Registrar ' + m.singular])
    ]));
    const aviso = avisoActual();
    if (aviso) $contenido.append(aviso);

    const lista = estado.datos[clave];
    const zonaTabla = el('div');
    const herramientas = el('div', { class: 'herramientas' }, [
        el('div', { class: 'izq' }, [
            el('div', { class: 'buscador' }, [icono('buscar'), el('input', {
                type: 'search', placeholder: 'Buscar ' + (m.menu || m.titulo).toLowerCase() + '…', value: estado.busqueda[clave] || '',
                'aria-label': 'Buscar', oninput: e => { estado.busqueda[clave] = e.target.value; pintarTabla(clave, zonaTabla); }
            })]),
            m.filtro ? filtroEstado(clave) : null
        ]),
        el('span', { class: 'total', id: 'total-' + clave })
    ]);
    $contenido.append(el('section', { class: 'panel' }, [herramientas, zonaTabla]));
    if (lista === null) {
        zonaTabla.append(vacio('No se pudo cargar la información', 'Verifique que la aplicación esté en ejecución.'));
        return;
    }
    pintarTabla(clave, zonaTabla);
}

function filtroEstado(clave) {
    const f = MODULOS[clave].filtro;
    const select = el('select', { 'aria-label': f.etiqueta, style: 'width:auto;min-width:170px', onchange: e => { estado.filtros[clave] = e.target.value; ir(clave); } },
        [el('option', { value: '', texto: 'Todos los estados' })].concat(f.opciones.map(([v, t]) => el('option', { value: v, texto: t }))));
    select.value = estado.filtros[clave] || '';
    return select;
}

function pintarTabla(clave, zona) {
    const m = MODULOS[clave];
    const q = (estado.busqueda[clave] || '').trim().toLowerCase();
    const todos = estado.datos[clave] || [];
    const lista = q ? todos.filter(item => Object.values(item).join(' ').toLowerCase().includes(q)) : todos;
    const total = document.getElementById('total-' + clave);
    if (total) total.textContent = lista.length + (lista.length === 1 ? ' registro' : ' registros');
    if (!lista.length) {
        zona.replaceChildren(q ? vacio('Sin coincidencias', 'No hay registros que coincidan con «' + q + '».')
            : estado.filtros[clave] ? vacio('Sin registros en este estado', 'Cambie el filtro para ver otros registros.')
                : vacio('Aún no hay registros', 'Use «Registrar ' + m.singular + '» para crear el primero.'));
        return;
    }
    const filas = lista.map(item => el('tr', { class: m.alClic ? 'clic' : '', onclick: m.alClic ? () => m.alClic(item) : null },
        m.columnas.map(([, render]) => el('td', {}, render(item))).concat(el('td', { class: 'derecha' }, el('div', { class: 'acciones-fila' }, accionesFila(clave, item))))));
    zona.replaceChildren(el('div', { class: 'tabla' }, el('table', {}, [
        el('thead', {}, el('tr', {}, m.columnas.map(([t]) => el('th', { texto: t })).concat(el('th', { class: 'derecha', texto: 'Acciones' })))),
        el('tbody', {}, filas)
    ])));
}

function accionesFila(clave, item) {
    const m = MODULOS[clave];
    if (m.accionesFila) return m.accionesFila(item);
    return [
        el('button', { class: 'boton secundario chico', onclick: () => abrirFormulario(clave, item) }, 'Editar'),
        el('button', { class: 'boton peligro chico', onclick: () => eliminar(clave, item.id) }, 'Eliminar')
    ];
}

// ------------------------------------------------------------------
// Ventana modal
// ------------------------------------------------------------------
function abrirModal(titulo, cuerpo, angosto) {
    cerrarModal();
    const capa = el('div', { class: 'capa', id: 'capa-modal', onmousedown: e => { if (e.target === capa) cerrarModal(); } }, el('div', { class: 'modal' + (angosto ? ' angosto' : ''), role: 'dialog', 'aria-modal': 'true', 'aria-label': titulo }, [
        el('div', { class: 'modal-cabecera' }, [el('h2', { texto: titulo }), el('button', { class: 'cerrar', 'aria-label': 'Cerrar', onclick: cerrarModal }, '×')]),
        el('div', { class: 'modal-cuerpo' }, cuerpo)
    ]));
    document.body.append(capa);
    capa.querySelector('input:not([type=hidden]), select, textarea')?.focus();
}
function cerrarModal() { document.getElementById('capa-modal')?.remove(); }
document.addEventListener('keydown', e => { if (e.key === 'Escape') cerrarModal(); });

async function abrirFormulario(clave, item, alGuardar) {
    const m = MODULOS[clave];
    const fuentes = [...new Set(m.campos.filter(c => c.tipo === 'relacion').map(c => c.fuente))];
    await cargarVarios(fuentes);
    const formulario = el('form', { class: 'formulario', novalidate: true, onsubmit: e => guardar(e, clave, item, alGuardar) });
    m.campos.forEach(campo => formulario.append(crearCampo(campo, item)));
    formulario.append(el('div', { class: 'acciones-form' }, [
        el('button', { type: 'button', class: 'boton secundario', onclick: cerrarModal }, 'Cancelar'),
        el('button', { type: 'submit', class: 'boton primario' }, item ? 'Guardar cambios' : 'Registrar ' + m.singular)
    ]));
    abrirModal((item ? 'Editar ' : (m.femenino ? 'Nueva ' : 'Nuevo ')) + m.singular, formulario);
}

function valorInicial(campo, item) {
    if (!item) return campo.valor;
    const valor = item[campo.nombre];
    if (campo.tipo === 'time') return hora(valor);
    return valor;
}

function crearCampo(campo, item) {
    const id = 'f-' + campo.nombre;
    const valor = valorInicial(campo, item);
    const contenedor = el('div', { class: 'campo' + (campo.ancho ? ' ancho' : ''), 'data-campo': campo.nombre });
    if (campo.tipo === 'check') {
        contenedor.append(el('label', { class: 'check' }, [el('input', { type: 'checkbox', name: campo.nombre, checked: !!valor }), campo.etiqueta]));
        return contenedor;
    }
    contenedor.append(el('label', { for: id, texto: campo.etiqueta + (campo.opcional ? ' (opcional)' : '') }));
    if (campo.tipo === 'dias') {
        const seleccion = item ? item.diasOperacion : DIAS.filter(d => d !== 'DOMINGO');
        contenedor.append(el('div', { class: 'selector-dias' }, DIAS.map(dia => el('label', {}, [
            el('input', { type: 'checkbox', name: 'diasOperacion', value: dia, checked: seleccion.includes(dia) }),
            dia === 'MIERCOLES' ? 'Miércoles' : dia === 'SABADO' ? 'Sábado' : dia.charAt(0) + dia.slice(1).toLowerCase()
        ]))));
    } else if (campo.tipo === 'select' || campo.tipo === 'relacion') {
        const select = el('select', { id, name: campo.nombre }, el('option', { value: '', texto: 'Seleccione…' }));
        if (campo.tipo === 'select') campo.opciones.forEach(op => select.append(el('option', { value: op, texto: op })));
        const opciones = campo.tipo === 'relacion' ? (estado.datos[campo.fuente] || []).filter(o => !campo.filtrar || campo.filtrar(o) || (item && o.id === item[campo.nombre])) : [];
        opciones.forEach(o => select.append(el('option', { value: o.id, texto: campo.mostrar(o) })));
        select.value = valor ?? (campo.tipo === 'select' ? campo.opciones[0] : '');
        contenedor.append(select);
        if (campo.tipo === 'relacion' && !opciones.length) {
            contenedor.append(el('span', { class: 'ayuda', texto: 'No hay registros disponibles. Registre primero: ' + MODULOS[campo.fuente].titulo.toLowerCase() + '.' }));
        }
    } else if (campo.tipo === 'textarea') {
        contenedor.append(el('textarea', { id, name: campo.nombre, texto: valor ?? '' }));
    } else {
        contenedor.append(el('input', { id, name: campo.nombre, type: campo.tipo || 'text', value: valor ?? '', step: campo.tipo === 'number' ? 'any' : null }));
    }
    if (campo.ayuda) contenedor.append(el('span', { class: 'ayuda', texto: campo.ayuda }));
    contenedor.append(el('small', { 'data-error': campo.nombre }));
    return contenedor;
}

function leerFormulario(formulario, campos) {
    const cuerpo = {};
    campos.forEach(campo => {
        if (campo.tipo === 'check') cuerpo[campo.nombre] = formulario.elements[campo.nombre].checked;
        else if (campo.tipo === 'dias') cuerpo[campo.nombre] = [...formulario.querySelectorAll('input[name=diasOperacion]:checked')].map(c => c.value);
        else {
            const valor = formulario.elements[campo.nombre].value.trim();
            const numerico = campo.tipo === 'number' || campo.tipo === 'relacion';
            if (valor === '') cuerpo[campo.nombre] = campo.opcional || numerico ? null : '';
            else cuerpo[campo.nombre] = numerico ? Number(valor) : valor;
        }
    });
    return cuerpo;
}

function mostrarErrores(formulario, error, alias = {}) {
    formulario.querySelectorAll('[data-error]').forEach(s => { s.textContent = ''; s.parentElement.classList.remove('con-error'); });
    formulario.querySelector('.aviso')?.remove();
    const sueltos = [];
    Object.entries(error.campos || {}).forEach(([campo, texto]) => {
        const destino = formulario.querySelector('[data-error="' + (alias[campo] || campo) + '"]');
        if (destino) { destino.textContent = texto; destino.parentElement.classList.add('con-error'); } else sueltos.push(texto);
    });
    formulario.prepend(el('div', { class: 'aviso error', role: 'alert', texto: error.message + (sueltos.length ? ': ' + sueltos.join(' ') : '') }));
}

async function guardar(evento, clave, item, alGuardar) {
    evento.preventDefault();
    const formulario = evento.target;
    const m = MODULOS[clave];
    try {
        await api(item ? 'PUT' : 'POST', m.api + (item ? '/' + item.id : ''), leerFormulario(formulario, m.campos));
        estado.mensaje = { tipo: 'exito', texto: (m.femenino ? 'La ' : 'El ') + m.singular + (item ? ' se actualizó' : ' se registró') + ' correctamente.' };
        cerrarModal();
        if (alGuardar) await alGuardar(); else await ir(clave, true);
    } catch (error) {
        mostrarErrores(formulario, error, { horarioValido: 'horaFin', periodoValido: 'fechaFin', recorridoValido: 'puntoDestino' });
    }
}

async function eliminar(clave, id, alTerminar) {
    const m = MODULOS[clave];
    if (!confirm('¿Desea eliminar ' + (m.femenino ? 'esta ' : 'este ') + m.singular + '? Esta acción no se puede deshacer.')) return;
    try {
        await api('DELETE', m.api + '/' + id);
        estado.mensaje = { tipo: 'exito', texto: 'Registro eliminado correctamente.' };
    } catch (error) {
        estado.mensaje = { tipo: 'error', texto: error.message };
    }
    if (alTerminar) await alTerminar(); else await ir(clave, true);
}

// ------------------------------------------------------------------
// Detalle de ruta: estado, asignacion, entregas y mapa
// ------------------------------------------------------------------
async function pintarDetalleRuta() {
    const id = estado.rutaId;
    let r, entregas;
    try {
        [r, entregas] = await Promise.all([api('GET', '/api/rutas/' + id), api('GET', '/api/entregas?rutaId=' + id)]);
    } catch (error) {
        $contenido.replaceChildren(vacio('No se pudo cargar la ruta', error.message));
        return;
    }
    migas('Planeación', 'Ruta ' + r.codigo);
    const recargar = () => ir('ruta', true);
    const pendientes = r.numeroEntregasProgramadas - r.entregasRealizadas - r.entregasNoRealizadas;
    const sinRecursos = !r.placaVehiculo || !r.conductorId;

    const acciones = [];
    if (r.estado !== 'FINALIZADA') {
        acciones.push(el('button', { class: 'boton secundario', onclick: () => abrirFormulario('rutas', r, recargar) }, 'Editar'));
        acciones.push(el('button', { class: 'boton secundario', onclick: () => abrirAsignacion(r) }, sinRecursos ? 'Asignar recursos' : 'Cambiar recursos'));
    }
    if (r.estado === 'PENDIENTE') {
        acciones.push(el('button', { class: 'boton peligro', onclick: () => eliminar('rutas', r.id, () => ir('rutas', true)) }, 'Eliminar'));
        acciones.push(el('button', { class: 'boton primario', disabled: sinRecursos, title: sinRecursos ? 'Asigne vehículo y conductor para iniciar' : null, onclick: () => cambiarEstado(r, 'iniciar') }, 'Iniciar ruta'));
    }
    if (r.estado === 'EN_CURSO') {
        acciones.push(el('button', { class: 'boton primario', disabled: pendientes <= 0, onclick: () => abrirRegistroEntrega(r.id, recargar) }, [icono('mas'), 'Registrar entrega']));
        acciones.push(el('button', { class: 'boton exito', onclick: () => cambiarEstado(r, 'finalizar', pendientes) }, 'Finalizar ruta'));
    }

    $contenido.replaceChildren(encabezado(r.codigo + ' · ' + r.nombre, r.nombreZona + ' · Contrato ' + r.codigoContrato + (r.razonSocialCliente ? ' · ' + r.razonSocialCliente : ''), acciones, ['Rutas', () => ir('rutas')]));
    const aviso = avisoActual();
    if (aviso) $contenido.append(aviso);
    if (r.estado === 'PENDIENTE' && sinRecursos) {
        $contenido.append(el('div', { class: 'aviso warn', texto: 'La ruta no tiene vehículo y conductor asignados. Asígnelos para poder iniciarla.' }));
    }

    // Linea de tiempo del estado de la ruta.
    const orden = ['PENDIENTE', 'EN_CURSO', 'FINALIZADA'];
    const actual = orden.indexOf(r.estado);
    const fechasEtapa = [fechaHora(r.fechaCreacion), r.fechaInicioReal ? fechaHora(r.fechaInicioReal) : 'Sin iniciar', r.fechaFinReal ? fechaHora(r.fechaFinReal) : 'Sin cerrar'];
    $contenido.append(el('section', { class: 'panel' }, el('div', { class: 'panel-cuerpo' }, el('ol', { class: 'etapas' }, orden.map((e, i) => {
        const clase = i < actual || r.estado === 'FINALIZADA' ? 'hecha' : i === actual ? 'actual' : '';
        return el('li', { class: clase }, [el('span', { class: 'circulo', texto: clase === 'hecha' ? '✓' : String(i + 1) }),
            el('span', {}, [ESTADOS_RUTA[e][0], el('span', { class: 'fecha', texto: (i === 0 ? 'Creada ' : '') + fechasEtapa[i] })])]);
    })))));

    // Columna izquierda: entregas y mapa.
    const p = r.numeroEntregasProgramadas;
    const panelEntregas = el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Entregas realizadas frente a programadas' }), leyendaEntregas()]),
        el('div', { class: 'panel-cuerpo' }, [
            el('div', { class: 'cifras' }, [
                cifra('Programadas', p), cifra('Entregadas', r.entregasRealizadas, 'var(--ok)'),
                cifra('No entregadas', r.entregasNoRealizadas, 'var(--danger)'), cifra('Pendientes', Math.max(0, pendientes))
            ]),
            el('div', { class: 'progreso' }, [
                el('div', { class: 'barra', style: 'height:10px' }, [
                    el('i', { class: 'ok', style: 'width:' + porcentaje(r.entregasRealizadas, p) + '%' }),
                    el('i', { class: 'danger', style: 'width:' + porcentaje(r.entregasNoRealizadas, p) + '%' })
                ]),
                el('span', { class: 'cifra num', texto: porcentaje(r.entregasRealizadas, p) + '% cumplido' })
            ])
        ]),
        tablaEntregas(entregas, r.estado === 'EN_CURSO', recargar, false)
    ]);

    const lienzo = el('div', { class: 'lienzo-mapa mini' });
    const pieMapa = el('div', { class: 'mapa-pie' });
    const panelMapa = el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Ubicación simulada del vehículo' }), r.estado === 'EN_CURSO' ? etiqueta('Actualización automática', 'info') : null]),
        lienzo, pieMapa
    ]);

    // Columna derecha: recursos y ficha tecnica.
    const panelRecursos = el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Recursos asignados' }),
            r.estado !== 'FINALIZADA' ? el('button', { class: 'enlace', onclick: () => abrirAsignacion(r) }, sinRecursos ? 'Asignar' : 'Cambiar') : null]),
        el('div', { class: 'panel-cuerpo' }, [
            recurso('vehiculos', 'Vehículo', r.placaVehiculo, r.descripcionVehiculo),
            recurso('conductores', 'Conductor', r.nombreConductor, r.nombreConductor ? 'Conductor asignado' : null)
        ])
    ]);
    const contrato = r.entregasEsperadasContrato;
    const panelFicha = el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, el('h2', { texto: 'Datos de la ruta' })),
        el('div', { class: 'panel-cuerpo', style: 'padding-top:4px' }, el('dl', { class: 'ficha' }, [
            dato('Punto de origen', r.puntoOrigen), dato('Punto de destino', r.puntoDestino),
            dato('Distancia estimada', miles(r.distanciaEstimadaKm) + ' km'), dato('Entregas programadas', miles(p)),
            dato('Zona', r.nombreZona), dato('Contrato', r.codigoContrato),
            dato('Entregas esperadas por el contrato', miles(contrato) + ' en total · ' + miles(r.entregasProgramadasContrato) + ' programadas en sus rutas', true),
            dato('Creada', fechaHora(r.fechaCreacion)), dato('Última actualización', fechaHora(r.fechaActualizacion))
        ]))
    ]);
    if (r.entregasProgramadasContrato > contrato) {
        panelFicha.lastChild.append(el('div', { class: 'aviso warn', style: 'margin:12px 0 0', texto: 'Las rutas de este contrato programan más entregas (' + miles(r.entregasProgramadasContrato) + ') de las esperadas por el cliente (' + miles(contrato) + ').' }));
    }

    $contenido.append(el('div', { class: 'rejilla detalle', style: 'margin-top:18px' }, [
        el('div', { class: 'rejilla' }, [panelEntregas, panelMapa]),
        el('div', { class: 'rejilla' }, [panelRecursos, panelFicha])
    ]));

    const refrescar = async () => {
        try {
            const u = await api('GET', '/api/rutas/' + id + '/ubicacion');
            MapaBogota.dibujar(lienzo, [u], { enfocar: true });
            pieMapa.replaceChildren(el('span', { texto: u.descripcionPosicion + (u.velocidadKmh ? ' · ' + u.velocidadKmh + ' km/h' : '') }),
                el('span', { class: 'num', texto: 'Avance del recorrido ' + Math.round(u.progreso * 100) + '% · Actualizado ' + u.calculadoEn.slice(11, 19) }));
        } catch (e) {
            pieMapa.textContent = e.message;
        }
    };
    await refrescar();
    if (r.estado === 'EN_CURSO') estado.temporizador = setInterval(() => { if (!document.getElementById('capa-modal')) refrescar(); }, REFRESCO_MAPA_MS);
}

function cifra(titulo, valor, color) {
    return el('div', {}, [el('div', { class: 't', texto: titulo }), el('div', { class: 'v num', style: color ? 'color:' + color : null, texto: miles(valor) })]);
}
function dato(titulo, valor, ancho) { return el('div', { class: ancho ? 'ancho' : null }, [el('dt', { texto: titulo }), el('dd', { texto: valor ?? '—' })]); }
function recurso(nombreIcono, titulo, principal, secundario) {
    return el('div', { class: 'recurso' }, [
        el('div', { class: 'icono' }, icono(nombreIcono)),
        el('div', {}, principal ? [el('strong', { texto: principal }), el('span', { texto: secundario || titulo })]
            : [el('strong', { class: 'sin', texto: titulo + ' sin asignar' }), el('span', { texto: 'Requerido para iniciar la ruta' })])
    ]);
}

async function cambiarEstado(r, accion, pendientes) {
    if (accion === 'finalizar') {
        const texto = pendientes > 0 ? 'Quedan ' + pendientes + ' entregas sin registrar. ¿Desea finalizar la ruta de todas formas?' : '¿Desea finalizar la ruta ' + r.codigo + '?';
        if (!confirm(texto)) return;
    }
    try {
        await api('PATCH', '/api/rutas/' + r.id + '/' + accion);
        estado.mensaje = { tipo: 'exito', texto: accion === 'iniciar' ? 'La ruta se inició. Ya puede registrar entregas.' : 'La ruta se finalizó correctamente.' };
    } catch (error) {
        estado.mensaje = { tipo: 'error', texto: error.message };
    }
    await ir('ruta', true);
}

async function abrirAsignacion(r) {
    const [vehiculos, conductores, rutas, necesidades] = await Promise.all([
        api('GET', '/api/vehiculos'), api('GET', '/api/conductores'), api('GET', '/api/rutas?estado=EN_CURSO'), api('GET', '/api/necesidades?operacionId=' + r.operacionId)
    ]).catch(error => { alert(error.message); return []; });
    if (!vehiculos) return;
    const operacion = (await cargar('operaciones'))?.find(o => o.id === r.operacionId);
    const ocupadoVehiculo = Object.fromEntries(rutas.filter(x => x.id !== r.id && x.vehiculoId).map(x => [x.vehiculoId, x.codigo]));
    const ocupadoConductor = Object.fromEntries(rutas.filter(x => x.id !== r.id && x.conductorId).map(x => [x.conductorId, x.codigo]));
    const frio = operacion?.requiereCadenaFrio;

    const motivoVehiculo = v => !v.activo ? 'inactivo' : frio && !v.requiereCadenaFrio ? 'sin cadena de frío' : ocupadoVehiculo[v.id] ? 'en ruta ' + ocupadoVehiculo[v.id] : null;
    const motivoConductor = c => !c.activo ? 'inactivo' : !licenciaVigente(c) ? 'licencia vencida' : ocupadoConductor[c.id] ? 'en ruta ' + ocupadoConductor[c.id] : null;
    const opcion = (valor, texto, motivo) => el('option', { value: valor, disabled: !!motivo, texto: texto + (motivo ? ' — ' + motivo : '') });

    const selVehiculo = el('select', { id: 'f-vehiculoId', name: 'vehiculoId' }, [el('option', { value: '', texto: 'Seleccione…' })]
        .concat(vehiculos.map(v => opcion(v.id, v.placa + ' · ' + v.tipoVehiculo + ' ' + miles(v.capacidadCargaKg) + ' kg' + (v.requiereCadenaFrio ? ' · frío' : ''), motivoVehiculo(v)))));
    const selConductor = el('select', { id: 'f-conductorId', name: 'conductorId' }, [el('option', { value: '', texto: 'Seleccione…' })]
        .concat(conductores.map(c => opcion(c.id, nombreConductor(c) + ' · ' + c.numeroLicencia, motivoConductor(c)))));
    selVehiculo.value = r.vehiculoId ?? '';
    selConductor.value = r.conductorId ?? '';

    const requisitos = necesidades.length ? el('div', { class: 'aviso info ancho' }, el('div', {}, [
        el('strong', { texto: 'Necesidad registrada por el cliente' }),
        ...necesidades.map(n => el('div', { texto: n.cantidadVehiculos + ' × ' + n.tipoVehiculo + ' (mín. ' + miles(n.capacidadMinimaKg) + ' kg' + (n.requiereCadenaFrio ? ', cadena de frío' : '') + ') · '
            + n.diasOperacion.map(d => INICIAL_DIA[d]).join(' ') + ' · ' + hora(n.horaInicio) + '–' + hora(n.horaFin) }))
    ])) : el('div', { class: 'aviso warn ancho', texto: 'El contrato no tiene necesidades registradas. Se recomienda registrarlas antes de asignar recursos.' });

    const formulario = el('form', { class: 'formulario', novalidate: true, onsubmit: async e => {
        e.preventDefault();
        try {
            await api('PATCH', '/api/rutas/' + r.id + '/asignacion', {
                vehiculoId: selVehiculo.value ? Number(selVehiculo.value) : null,
                conductorId: selConductor.value ? Number(selConductor.value) : null
            });
            estado.mensaje = { tipo: 'exito', texto: 'Vehículo y conductor asignados a la ruta ' + r.codigo + '.' };
            cerrarModal();
            await ir('ruta', true);
        } catch (error) { mostrarErrores(formulario, error); }
    } }, [
        requisitos,
        el('div', { class: 'campo ancho' }, [el('label', { for: 'f-vehiculoId', texto: 'Vehículo' }), selVehiculo,
            frio ? el('span', { class: 'ayuda', texto: 'La operación requiere vehículos con cadena de frío.' }) : null, el('small', { 'data-error': 'vehiculoId' })]),
        el('div', { class: 'campo ancho' }, [el('label', { for: 'f-conductorId', texto: 'Conductor' }), selConductor,
            el('span', { class: 'ayuda', texto: 'Solo conductores activos con licencia vigente y sin otra ruta en curso.' }), el('small', { 'data-error': 'conductorId' })]),
        el('div', { class: 'acciones-form' }, [
            el('button', { type: 'button', class: 'boton secundario', onclick: cerrarModal }, 'Cancelar'),
            el('button', { type: 'submit', class: 'boton primario' }, 'Guardar asignación')
        ])
    ]);
    abrirModal('Asignar recursos · ' + r.codigo, formulario, true);
}

// ------------------------------------------------------------------
// Registro de entregas
// ------------------------------------------------------------------
const CAMPOS_ENTREGA = [
    { nombre: 'numeroGuia', etiqueta: 'Número de guía', opcional: true },
    { nombre: 'destinatario', etiqueta: 'Destinatario' },
    { nombre: 'direccion', etiqueta: 'Dirección de entrega', ancho: true },
    { nombre: 'estado', etiqueta: 'Resultado', tipo: 'select', opciones: ['ENTREGADA', 'NO_ENTREGADA'] },
    { nombre: 'novedad', etiqueta: 'Novedad', tipo: 'textarea', opcional: true, ancho: true, ayuda: 'Obligatoria si la entrega no se realizó (p. ej. cliente ausente, dirección errada).' }
];

async function abrirRegistroEntrega(rutaId, alGuardar) {
    let rutasEnCurso = [];
    if (!rutaId) {
        rutasEnCurso = (await api('GET', '/api/rutas?estado=EN_CURSO').catch(() => []))
            .filter(r => r.entregasRealizadas + r.entregasNoRealizadas < r.numeroEntregasProgramadas);
    }
    const formulario = el('form', { class: 'formulario', novalidate: true });
    if (!rutaId) {
        const select = el('select', { id: 'f-rutaId', name: 'rutaId' }, [el('option', { value: '', texto: 'Seleccione…' })]
            .concat(rutasEnCurso.map(r => el('option', { value: r.id, texto: r.codigo + ' · ' + r.nombre + ' (' + (r.entregasRealizadas + r.entregasNoRealizadas) + '/' + r.numeroEntregasProgramadas + ')' }))));
        formulario.append(el('div', { class: 'campo ancho' }, [el('label', { for: 'f-rutaId', texto: 'Ruta en curso' }), select,
            rutasEnCurso.length ? null : el('span', { class: 'ayuda', texto: 'No hay rutas en curso con entregas pendientes.' }), el('small', { 'data-error': 'rutaId' })]));
    }
    CAMPOS_ENTREGA.forEach(c => formulario.append(crearCampo(c)));
    const selEstado = formulario.elements.estado;
    [...selEstado.options].forEach(o => { if (o.value) o.textContent = o.value === 'ENTREGADA' ? 'Entregada' : 'No entregada (con novedad)'; });
    formulario.append(el('div', { class: 'acciones-form' }, [
        el('button', { type: 'button', class: 'boton secundario', onclick: cerrarModal }, 'Cancelar'),
        el('button', { type: 'submit', class: 'boton primario' }, 'Registrar entrega')
    ]));
    formulario.addEventListener('submit', async e => {
        e.preventDefault();
        const cuerpo = leerFormulario(formulario, CAMPOS_ENTREGA);
        cuerpo.rutaId = rutaId || (formulario.elements.rutaId.value ? Number(formulario.elements.rutaId.value) : null);
        try {
            await api('POST', '/api/entregas', cuerpo);
            estado.mensaje = { tipo: 'exito', texto: 'Entrega registrada correctamente.' };
            cerrarModal();
            await alGuardar();
        } catch (error) { mostrarErrores(formulario, error, { novedadValida: 'novedad' }); }
    });
    abrirModal('Registrar entrega', formulario);
}

function tablaEntregas(entregas, corregible, alCambiar, mostrarRuta) {
    if (!entregas.length) return vacio('Sin entregas registradas', corregible ? 'Registre cada entrega a medida que el conductor la confirme.' : 'Las entregas se registran mientras la ruta está en curso.');
    return el('div', { class: 'tabla' }, el('table', {}, [
        el('thead', {}, el('tr', {}, [mostrarRuta ? 'Ruta' : '#', 'Destinatario', 'Guía', 'Resultado', 'Registro', ''].map(t => el('th', { texto: t })))),
        el('tbody', {}, entregas.map((e, i) => el('tr', {}, [
            el('td', { class: 'num' }, mostrarRuta ? el('button', { class: 'enlace', onclick: () => irDetalleRuta(e.rutaId) }, e.codigoRuta) : String(i + 1)),
            el('td', {}, doble(e.destinatario, e.direccion)),
            el('td', { class: 'num', texto: e.numeroGuia || '—' }),
            el('td', {}, el('div', {}, [e.estado === 'ENTREGADA' ? etiqueta('Entregada', 'ok') : etiqueta('No entregada', 'danger'), e.novedad ? el('span', { class: 'sec', texto: e.novedad }) : null])),
            el('td', { class: 'num', texto: fechaHora(e.fechaRegistro) }),
            el('td', { class: 'derecha' }, corregible ? el('button', { class: 'boton peligro chico', onclick: () => eliminar('entregas', e.id, alCambiar) }, 'Anular') : null)
        ])))
    ]));
}

async function pintarEntregas() {
    const m = MODULOS.entregas;
    migas(m.grupo, m.titulo);
    const [entregas, rutas] = await Promise.all([cargar('entregas'), cargar('rutas')]);
    $contenido.replaceChildren(encabezado(m.titulo, m.descripcion, [
        el('button', { class: 'boton primario', onclick: () => abrirRegistroEntrega(null, () => ir('entregas', true)) }, [icono('mas'), 'Registrar entrega'])
    ]));
    const aviso = avisoActual();
    if (aviso) $contenido.append(aviso);
    if (!entregas || !rutas) { $contenido.append(vacio('No se pudo cargar la información', 'Verifique que la aplicación esté en ejecución.')); return; }

    const iniciadas = rutas.filter(r => r.estado !== 'PENDIENTE');
    const programadas = iniciadas.reduce((s, r) => s + r.numeroEntregasProgramadas, 0);
    const ok = entregas.filter(e => e.estado === 'ENTREGADA').length;
    const novedad = entregas.length - ok;
    $contenido.append(el('section', { class: 'kpis' }, [
        kpi('Programadas', miles(programadas), 'En rutas iniciadas o finalizadas', '#16355a'),
        kpi('Entregadas', miles(ok), porcentaje(ok, programadas) + '% de lo programado', '#0f7b4f'),
        kpi('No entregadas', miles(novedad), 'Registradas con novedad', '#b3261e'),
        kpi('Pendientes', miles(Math.max(0, programadas - entregas.length)), 'Aún sin registrar', '#55627a')
    ]));

    // Comparativo por ruta: realizadas frente a programadas.
    $contenido.append(el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Cumplimiento por ruta' }), leyendaEntregas()]),
        iniciadas.length ? el('div', { class: 'tabla' }, el('table', {}, [
            el('thead', {}, el('tr', {}, ['Ruta', 'Estado', 'Programadas', 'Entregadas', 'No entregadas', 'Avance'].map(t => el('th', { texto: t })))),
            el('tbody', {}, iniciadas.map(r => el('tr', { class: 'clic', onclick: () => irDetalleRuta(r.id) }, [
                el('td', {}, doble(r.codigo, r.nombre)), el('td', {}, estadoRuta(r.estado)),
                el('td', { class: 'num', texto: r.numeroEntregasProgramadas }), el('td', { class: 'num', texto: r.entregasRealizadas }),
                el('td', { class: 'num', texto: r.entregasNoRealizadas }), el('td', {}, barraEntregas(r, 200))
            ])))
        ])) : vacio('No hay rutas iniciadas', 'Inicie una ruta desde su detalle para registrar entregas.')
    ]));

    const filtro = estado.filtros.entregas || '';
    const select = el('select', { 'aria-label': 'Filtrar por ruta', style: 'width:auto;min-width:200px', onchange: e => { estado.filtros.entregas = e.target.value; pintarHistorial(); } },
        [el('option', { value: '', texto: 'Todas las rutas' })].concat(iniciadas.map(r => el('option', { value: r.id, texto: r.codigo + ' · ' + r.nombre }))));
    select.value = filtro;
    const zona = el('div');
    const total = el('span', { class: 'total' });
    const pintarHistorial = () => {
        const id = Number(estado.filtros.entregas || 0);
        const lista = id ? entregas.filter(e => e.rutaId === id) : entregas;
        total.textContent = lista.length + ' registros';
        const enCurso = new Set(rutas.filter(r => r.estado === 'EN_CURSO').map(r => r.id));
        zona.replaceChildren(lista.length ? tablaEntregasGlobal(lista, enCurso) : vacio('Sin entregas registradas', 'Las entregas aparecerán aquí a medida que se registren.'));
    };
    $contenido.append(el('section', { class: 'panel' }, [
        el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Historial de entregas' }), el('div', { class: 'leyenda' }, [select, total])]),
        zona
    ]));
    pintarHistorial();
}

function tablaEntregasGlobal(lista, rutasEnCurso) {
    const tabla = tablaEntregas(lista, false, null, true);
    // Solo las entregas de rutas en curso se pueden anular.
    tabla.querySelectorAll('tbody tr').forEach((fila, i) => {
        const e = lista[i];
        if (rutasEnCurso.has(e.rutaId)) {
            fila.lastChild.append(el('button', { class: 'boton peligro chico', onclick: () => eliminar('entregas', e.id, () => ir('entregas', true)) }, 'Anular'));
        }
    });
    return tabla;
}

// ------------------------------------------------------------------
// Monitoreo en mapa
// ------------------------------------------------------------------
async function pintarMonitoreo() {
    migas('General', 'Monitoreo de flota');
    const filtro = el('select', { 'aria-label': 'Rutas a mostrar', style: 'width:auto', onchange: e => { estado.filtroMapa = e.target.value; estado.seleccionMapa = null; refrescar(); } }, [
        el('option', { value: 'EN_CURSO', texto: 'Solo rutas en curso' }),
        el('option', { value: 'ACTIVAS', texto: 'Pendientes y en curso' }),
        el('option', { value: 'TODAS', texto: 'Todas las rutas' })
    ]);
    filtro.value = estado.filtroMapa;
    const marcaTiempo = el('span', { class: 'total num' });
    $contenido.replaceChildren(encabezado('Monitoreo de flota', 'Ubicación simulada de los vehículos según el avance de sus entregas. Se actualiza cada ' + REFRESCO_MAPA_MS / 1000 + ' segundos.', [filtro]));

    const lienzo = el('div', { class: 'lienzo-mapa' });
    const lista = el('div', { class: 'unidades' });
    const contador = el('span', { class: 'total' });
    $contenido.append(el('div', { class: 'monitoreo' }, [
        el('section', { class: 'panel' }, [
            el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Plano de Bogotá D.C.' }), marcaTiempo]),
            lienzo,
            el('div', { class: 'mapa-pie' }, [
                el('div', { class: 'leyenda' }, [
                    el('span', {}, [el('i', { style: 'background:#102741' }), 'Centro de distribución']),
                    el('span', {}, [el('i', { style: 'background:#0f7b4f;border-radius:50%' }), 'Entregada']),
                    el('span', {}, [el('i', { style: 'background:#c0392b;border-radius:50%' }), 'No entregada']),
                    el('span', {}, [el('i', { style: 'background:#fff;border:1.5px solid #6b778c;border-radius:50%' }), 'Pendiente'])
                ]),
                el('span', { texto: 'Plano esquemático · posiciones simuladas, no GPS' })
            ])
        ]),
        el('section', { class: 'panel' }, [el('div', { class: 'panel-cabecera' }, [el('h2', { texto: 'Unidades' }), contador]), lista])
    ]));

    const seleccionar = id => { estado.seleccionMapa = estado.seleccionMapa === id ? null : id; dibujar(); };
    let ultimas = [];
    let colores = {};
    const dibujar = () => {
        const visibles = ultimas.filter(u => estado.filtroMapa === 'TODAS' || u.estado === 'EN_CURSO' || (estado.filtroMapa === 'ACTIVAS' && u.estado === 'PENDIENTE'));
        if (estado.seleccionMapa !== null && !visibles.some(u => u.rutaId === estado.seleccionMapa)) estado.seleccionMapa = null;
        MapaBogota.dibujar(lienzo, visibles, { seleccion: estado.seleccionMapa, colores, alSeleccionar: seleccionar });
        contador.textContent = visibles.length + (visibles.length === 1 ? ' ruta' : ' rutas');
        lista.replaceChildren(...(visibles.length ? visibles.map(u => el('div', { class: 'unidad' + (estado.seleccionMapa === u.rutaId ? ' sel' : ''), onclick: () => seleccionar(u.rutaId) }, [
            el('div', { class: 'fila' }, [el('strong', {}, [el('span', { class: 'color', style: 'background:' + colores[u.rutaId] + ';margin-right:8px' }), u.placaVehiculo || 'Sin vehículo']), estadoRuta(u.estado)]),
            el('div', { class: 'desc', texto: u.codigo + ' · ' + u.nombre + (u.nombreConductor ? ' · ' + u.nombreConductor : '') }),
            el('div', { class: 'desc', texto: u.descripcionPosicion + (u.velocidadKmh ? ' · ' + u.velocidadKmh + ' km/h' : '') }),
            el('div', { class: 'fila' }, [
                el('div', { class: 'progreso', style: 'flex:1' }, [el('div', { class: 'barra' }, el('i', { class: 'info', style: 'width:' + Math.round(u.progreso * 100) + '%' })),
                    el('span', { class: 'cifra num', texto: u.entregasRegistradas + '/' + u.entregasProgramadas })]),
                el('button', { class: 'enlace', onclick: e => { e.stopPropagation(); irDetalleRuta(u.rutaId); } }, 'Detalle')
            ])
        ])) : [vacio('Sin rutas para mostrar', estado.filtroMapa === 'EN_CURSO' ? 'Inicie una ruta para verla en el mapa.' : 'Registre rutas para verlas en el mapa.')]));
    };
    const refrescar = async () => {
        try {
            ultimas = await api('GET', '/api/rutas/ubicaciones');
            colores = Object.fromEntries(ultimas.map((u, i) => [u.rutaId, MapaBogota.color(i)]));
            marcaTiempo.textContent = 'Actualizado ' + new Date().toLocaleTimeString('es-CO');
        } catch (e) {
            marcaTiempo.textContent = e.message;
        }
        dibujar();
    };
    await refrescar();
    estado.temporizador = setInterval(refrescar, REFRESCO_MAPA_MS);
}

// ------------------------------------------------------------------
// Arranque
// ------------------------------------------------------------------
document.getElementById('fecha-actual').textContent = new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
const [vistaInicial, idInicial] = location.hash.slice(1).split('/');
if (vistaInicial === 'ruta' && Number(idInicial)) irDetalleRuta(Number(idInicial));
else ir(VISTAS_GENERALES[vistaInicial] || MODULOS[vistaInicial] ? vistaInicial : 'tablero');
