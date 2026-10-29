// ------------------------------------------------------------------
// Plano esquematico de Bogota dibujado en SVG. Funciona sin internet:
// no usa mosaicos externos, solo coordenadas aproximadas de la ciudad.
// ------------------------------------------------------------------
const MapaBogota = (() => {
    const LIMITES = { latMin: 4.48, latMax: 4.78, lngMin: -74.25, lngMax: -74.00 };
    const ANCHO = 1000;
    const ALTO = Math.round(ANCHO * (LIMITES.latMax - LIMITES.latMin) / (LIMITES.lngMax - LIMITES.lngMin));
    const COLORES = ['#1d4ed8', '#b45309', '#047857', '#7c3aed', '#be123c', '#0e7490', '#4d7c0f', '#a21caf'];

    const AREA_URBANA = [[4.775, -74.035], [4.775, -74.095], [4.745, -74.125], [4.720, -74.150], [4.690, -74.165],
        [4.660, -74.190], [4.640, -74.205], [4.610, -74.215], [4.590, -74.205], [4.560, -74.180], [4.535, -74.165],
        [4.500, -74.140], [4.485, -74.120], [4.500, -74.095], [4.540, -74.085], [4.560, -74.070], [4.590, -74.060],
        [4.620, -74.055], [4.660, -74.045], [4.700, -74.025], [4.740, -74.020]];
    const CERROS = [[4.78, -74.018], [4.70, -74.020], [4.66, -74.042], [4.62, -74.052], [4.58, -74.058], [4.54, -74.080],
        [4.48, -74.090], [4.48, -74.00], [4.78, -74.00]];
    const RIO = [[4.78, -74.10], [4.74, -74.13], [4.71, -74.17], [4.68, -74.19], [4.65, -74.21], [4.62, -74.225],
        [4.59, -74.215], [4.56, -74.23], [4.53, -74.245]];
    const VIAS = [
        ['Autopista Norte', [[4.775, -74.045], [4.68, -74.058]]],
        ['Av. Caracas', [[4.68, -74.058], [4.62, -74.068], [4.57, -74.10], [4.50, -74.115]]],
        ['Carrera Séptima', [[4.775, -74.030], [4.70, -74.035], [4.64, -74.060], [4.60, -74.070]]],
        ['NQS', [[4.70, -74.065], [4.64, -74.083], [4.60, -74.11], [4.58, -74.16], [4.59, -74.21]]],
        ['Av. Boyacá', [[4.77, -74.07], [4.70, -74.115], [4.64, -74.14], [4.60, -74.15], [4.55, -74.15]]],
        ['Calle 80', [[4.665, -74.06], [4.70, -74.10], [4.735, -74.15]]],
        ['Av. El Dorado', [[4.615, -74.07], [4.65, -74.11], [4.69, -74.15]]],
        ['Av. Américas', [[4.615, -74.10], [4.63, -74.15], [4.625, -74.19]]],
        ['Calle 13', [[4.605, -74.085], [4.645, -74.145], [4.69, -74.205]]],
        ['Av. Ciudad de Cali', [[4.74, -74.10], [4.66, -74.14], [4.62, -74.165], [4.60, -74.18]]]
    ];
    const LOCALIDADES = [['Usaquén', 4.7110, -74.0300], ['Chapinero', 4.6480, -74.0620], ['Santa Fe', 4.6100, -74.0700],
        ['San Cristóbal', 4.5650, -74.0880], ['Usme', 4.5000, -74.1100], ['Tunjuelito', 4.5760, -74.1330],
        ['Bosa', 4.6180, -74.1880], ['Kennedy', 4.6300, -74.1550], ['Fontibón', 4.6740, -74.1450],
        ['Engativá', 4.7070, -74.1100], ['Suba', 4.7420, -74.0840], ['Barrios Unidos', 4.6680, -74.0760],
        ['Teusaquillo', 4.6380, -74.0850], ['Los Mártires', 4.6050, -74.0920], ['Antonio Nariño', 4.5880, -74.1020],
        ['Puente Aranda', 4.6170, -74.1170], ['Rafael Uribe', 4.5700, -74.1160], ['Ciudad Bolívar', 4.5450, -74.1500],
        ['Soacha', 4.5790, -74.2170], ['Funza', 4.7160, -74.2110]];

    const x = lng => (lng - LIMITES.lngMin) / (LIMITES.lngMax - LIMITES.lngMin) * ANCHO;
    const y = lat => (LIMITES.latMax - lat) / (LIMITES.latMax - LIMITES.latMin) * ALTO;
    const xy = p => x(p.longitud ?? p[1]).toFixed(1) + ',' + y(p.latitud ?? p[0]).toFixed(1);
    const esc = t => String(t ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const color = indice => COLORES[indice % COLORES.length];

    function fondo(escala) {
        const partes = [];
        partes.push('<rect x="0" y="0" width="' + ANCHO + '" height="' + ALTO + '" fill="#e9edf1"/>');
        // Cuadricula cada 0.02 grados (aprox. 2.2 km).
        for (let lat = 4.50; lat < LIMITES.latMax; lat += 0.02) {
            partes.push('<line x1="0" x2="' + ANCHO + '" y1="' + y(lat).toFixed(1) + '" y2="' + y(lat).toFixed(1) + '" stroke="#dfe4ea" stroke-width="' + escala + '"/>');
        }
        for (let lng = -74.24; lng < LIMITES.lngMax; lng += 0.02) {
            partes.push('<line y1="0" y2="' + ALTO + '" x1="' + x(lng).toFixed(1) + '" x2="' + x(lng).toFixed(1) + '" stroke="#dfe4ea" stroke-width="' + escala + '"/>');
        }
        partes.push('<polygon points="' + AREA_URBANA.map(xy).join(' ') + '" fill="#f8f9fa" stroke="#cfd6de" stroke-width="' + (1.5 * escala) + '"/>');
        partes.push('<polygon points="' + CERROS.map(xy).join(' ') + '" fill="#dbe5d6" stroke="#c4d2bd" stroke-width="' + escala + '"/>');
        partes.push('<polyline points="' + RIO.map(xy).join(' ') + '" fill="none" stroke="#a9c7e3" stroke-width="' + (5 * escala) + '" stroke-linecap="round" stroke-linejoin="round"/>');
        VIAS.forEach(([, puntos]) => partes.push('<polyline points="' + puntos.map(xy).join(' ') + '" fill="none" stroke="#ffffff" stroke-width="' + (6 * escala) + '" stroke-linecap="round" stroke-linejoin="round"/>'));
        VIAS.forEach(([, puntos]) => partes.push('<polyline points="' + puntos.map(xy).join(' ') + '" fill="none" stroke="#d3d9e0" stroke-width="' + (1.2 * escala) + '" stroke-linecap="round" stroke-linejoin="round"/>'));
        LOCALIDADES.forEach(([nombre, lat, lng]) => partes.push('<text x="' + x(lng).toFixed(1) + '" y="' + y(lat).toFixed(1)
            + '" text-anchor="middle" font-size="' + (13 * escala) + '" fill="#8b97a8" font-weight="600" letter-spacing="' + (0.6 * escala) + '">' + esc(nombre.toUpperCase()) + '</text>'));
        partes.push('<text x="' + x(-74.215) + '" y="' + y(4.765) + '" font-size="' + (12 * escala) + '" fill="#7e9cbc" font-style="italic">Río Bogotá</text>');
        partes.push('<text x="' + x(-74.012) + '" y="' + y(4.53) + '" font-size="' + (12 * escala) + '" fill="#8aa182" font-style="italic" transform="rotate(-70 ' + x(-74.012) + ' ' + y(4.53) + ')">Cerros Orientales</text>');
        return partes.join('');
    }

    function cedi(ubicaciones, escala) {
        const origen = ubicaciones.length ? ubicaciones[0].origen : { latitud: 4.6330, longitud: -74.1180 };
        const cx = x(origen.longitud), cy = y(origen.latitud), t = 11 * escala;
        return '<g><rect x="' + (cx - t) + '" y="' + (cy - t) + '" width="' + (2 * t) + '" height="' + (2 * t) + '" rx="' + (3 * escala) + '" fill="#102741"/>'
            + '<text x="' + cx + '" y="' + (cy + 4 * escala) + '" text-anchor="middle" font-size="' + (10 * escala) + '" fill="#fff" font-weight="700">CD</text>'
            + '<text x="' + cx + '" y="' + (cy + t + 15 * escala) + '" text-anchor="middle" font-size="' + (12 * escala) + '" fill="#102741" font-weight="700">Centro de distribución</text></g>';
    }

    function ruta(u, tono, escala, resaltada, atenuada) {
        const puntos = [u.origen, ...u.paradas, u.destino];
        const recorridos = Math.min(u.entregasRegistradas, u.paradas.length);
        const opacidad = atenuada ? 0.25 : 1;
        const partes = ['<g opacity="' + opacidad + '" data-ruta="' + u.rutaId + '" style="cursor:pointer">'];
        partes.push('<polyline points="' + puntos.map(xy).join(' ') + '" fill="none" stroke="' + tono + '" stroke-width="' + ((resaltada ? 3 : 2) * escala)
            + '" stroke-dasharray="' + (6 * escala) + ' ' + (5 * escala) + '" stroke-opacity=".55" stroke-linejoin="round"/>');
        // Tramo ya recorrido: desde el CEDI hasta la posicion actual del vehiculo.
        if (u.estado !== 'PENDIENTE') {
            const hecho = puntos.slice(0, recorridos + 1).concat(u.estado === 'FINALIZADA' ? puntos.slice(recorridos + 1) : [u.vehiculo]);
            partes.push('<polyline points="' + hecho.map(xy).join(' ') + '" fill="none" stroke="' + tono + '" stroke-width="' + ((resaltada ? 4.5 : 3) * escala) + '" stroke-linejoin="round" stroke-linecap="round"/>');
        }
        u.paradas.forEach((p, i) => {
            const relleno = p.estado === 'ENTREGADA' ? '#0f7b4f' : p.estado === 'NO_ENTREGADA' ? '#c0392b' : '#ffffff';
            partes.push('<circle cx="' + x(p.longitud).toFixed(1) + '" cy="' + y(p.latitud).toFixed(1) + '" r="' + (5 * escala) + '" fill="' + relleno
                + '" stroke="' + (p.estado === 'PENDIENTE' ? tono : '#ffffff') + '" stroke-width="' + (1.8 * escala) + '"><title>' + esc(u.codigo + ' · ' + p.etiqueta + ' · ' + p.estado.replace('_', ' ')) + '</title></circle>');
            if (resaltada && u.paradas.length <= 30) {
                partes.push('<text x="' + (x(p.longitud) + 8 * escala).toFixed(1) + '" y="' + (y(p.latitud) - 6 * escala).toFixed(1) + '" font-size="' + (10 * escala) + '" fill="#44526a">' + (i + 1) + '</text>');
            }
        });
        const d = u.destino;
        partes.push('<path transform="translate(' + x(d.longitud).toFixed(1) + ' ' + y(d.latitud).toFixed(1) + ') scale(' + escala + ')" d="M0 0 L0 -22 L14 -17 L0 -12" fill="' + tono + '" stroke="' + tono + '" stroke-width="2" stroke-linejoin="round"><title>' + esc('Destino: ' + d.etiqueta) + '</title></path>');
        partes.push('</g>');
        return partes.join('');
    }

    function vehiculo(u, tono, escala) {
        const cx = x(u.vehiculo.longitud), cy = y(u.vehiculo.latitud);
        const texto = u.vehiculo.etiqueta;
        const ancho = (texto.length * 7.2 + 16) * escala, alto = 20 * escala;
        const movimiento = u.estado === 'EN_CURSO' && u.velocidadKmh > 0;
        return '<g data-ruta="' + u.rutaId + '" style="cursor:pointer">'
            + (movimiento ? '<circle class="pulso" cx="' + cx.toFixed(1) + '" cy="' + cy.toFixed(1) + '" r="9" fill="' + tono + '" style="transform-box: fill-box"/>' : '')
            + '<circle cx="' + cx.toFixed(1) + '" cy="' + cy.toFixed(1) + '" r="' + (7 * escala) + '" fill="' + tono + '" stroke="#fff" stroke-width="' + (2.5 * escala) + '"/>'
            + '<rect x="' + (cx - ancho / 2).toFixed(1) + '" y="' + (cy - alto - 12 * escala).toFixed(1) + '" width="' + ancho.toFixed(1) + '" height="' + alto.toFixed(1) + '" rx="' + (3 * escala) + '" fill="#102741"/>'
            + '<text x="' + cx.toFixed(1) + '" y="' + (cy - 12 * escala - 6 * escala).toFixed(1) + '" text-anchor="middle" font-size="' + (11.5 * escala) + '" fill="#fff" font-weight="700" letter-spacing="' + (0.5 * escala) + '">' + esc(texto) + '</text>'
            + '<title>' + esc(u.codigo + ' · ' + u.descripcionPosicion) + '</title></g>';
    }

    // Recorta la vista a las rutas mostradas cuando se pide un mapa enfocado.
    function caja(ubicaciones) {
        const puntos = ubicaciones.flatMap(u => [u.origen, u.destino, u.vehiculo, ...u.paradas]);
        if (!puntos.length) return [0, 0, ANCHO, ALTO];
        const xs = puntos.map(p => x(p.longitud)), ys = puntos.map(p => y(p.latitud));
        let minX = Math.min(...xs), maxX = Math.max(...xs), minY = Math.min(...ys), maxY = Math.max(...ys);
        const margen = 60;
        let ancho = Math.max(maxX - minX + margen * 2, 320), alto = Math.max(maxY - minY + margen * 2, 240);
        // Mantiene una proporcion apaisada para que el mapa no quede estrecho.
        if (ancho / alto < 1.35) ancho = alto * 1.35; else alto = ancho / 1.35;
        const cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        return [cx - ancho / 2, cy - alto / 2, ancho, alto];
    }

    function dibujar(contenedor, ubicaciones, opciones = {}) {
        const vista = opciones.enfocar ? caja(ubicaciones) : [0, 0, ANCHO, ALTO];
        const escala = Math.max(0.45, vista[2] / ANCHO);
        const seleccion = opciones.seleccion ?? null;
        const colores = opciones.colores || {};
        const rutas = ubicaciones.map((u, i) => {
            const tono = colores[u.rutaId] || color(i);
            return ruta(u, tono, escala, seleccion === u.rutaId || ubicaciones.length === 1, seleccion !== null && seleccion !== u.rutaId);
        }).join('');
        const vehiculos = ubicaciones.filter(u => seleccion === null || seleccion === u.rutaId)
            .map((u, i) => vehiculo(u, colores[u.rutaId] || color(ubicaciones.indexOf(u)), escala)).join('');
        // Norte y escala grafica (0.02 grados de longitud ~ 2.2 km en Bogota).
        const nx = vista[0] + vista[2] - 40 * escala, ny = vista[1] + 50 * escala;
        const brujula = '<g transform="translate(' + nx + ' ' + ny + ') scale(' + escala + ')"><circle r="18" fill="#fff" stroke="#c9d2de"/><path d="M0 -13 L6 5 L0 1 L-6 5 Z" fill="#102741"/><text y="-21" text-anchor="middle" font-size="11" font-weight="700" fill="#102741">N</text></g>';
        const ex = vista[0] + 24 * escala, ey = vista[1] + vista[3] - 24 * escala, largo = x(-74.18) - x(-74.20);
        const regla = '<g><rect x="' + ex + '" y="' + (ey - 6 * escala) + '" width="' + largo + '" height="' + (6 * escala) + '" fill="#102741"/><rect x="' + (ex + largo / 2) + '" y="' + (ey - 6 * escala) + '" width="' + (largo / 2) + '" height="' + (6 * escala) + '" fill="#fff" stroke="#102741" stroke-width="' + escala + '"/>'
            + '<text x="' + ex + '" y="' + (ey - 11 * escala) + '" font-size="' + (11 * escala) + '" fill="#44526a">0</text><text x="' + (ex + largo) + '" y="' + (ey - 11 * escala) + '" font-size="' + (11 * escala) + '" fill="#44526a" text-anchor="end">2,2 km</text></g>';
        contenedor.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' + vista.map(v => v.toFixed(1)).join(' ') + '" role="img" aria-label="Mapa de ubicación simulada" font-family="Segoe UI, Arial, sans-serif">'
            + fondo(escala) + cedi(ubicaciones, escala) + rutas + vehiculos + brujula + regla + '</svg>';
        if (opciones.alSeleccionar) {
            contenedor.querySelectorAll('[data-ruta]').forEach(nodo => nodo.addEventListener('click', () => opciones.alSeleccionar(Number(nodo.dataset.ruta))));
        }
    }

    return { dibujar, color: color };
})();
