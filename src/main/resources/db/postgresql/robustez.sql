-- =====================================================================
-- SpaceTrack - Robustez del esquema en PostgreSQL (Neon)
-- Hibernate (ddl-auto=update) crea tablas, PK, FK y UNIQUE; este script
-- agrega lo que Hibernate no crea: indices de llaves foraneas, reglas de
-- dominio (CHECK) e integridad entre filas (indices unicos parciales).
-- Es idempotente: se ejecuta en cada arranque del perfil prod.
-- Separador de sentencias: @@ (los bloques DO contienen ';').
--
-- Modelo de relaciones:
--   clientes            1 ── N  operaciones         (operaciones.cliente_id)
--   operaciones         1 ── N  necesidades_cliente (necesidades_cliente.operacion_id)
--   necesidades_cliente 1 ── N  necesidad_dias      (PK compuesta necesidad_id + dia)
--   operaciones         1 ── N  rutas               (rutas.operacion_id)
--   zonas               1 ── N  rutas               (rutas.zona_id)
--   vehiculos           1 ── N  rutas  (0..1 ruta EN_CURSO por vehiculo a la vez)
--   conductores         1 ── N  rutas  (0..1 ruta EN_CURSO por conductor a la vez)
--   rutas               1 ── N  entregas            (entregas.ruta_id)
--   rutas               1 ── N  posiciones_ruta     (posiciones_ruta.ruta_id)
--   usuarios_app: 1 ── 1 con neon_auth.user mediante auth_id (sub del JWT)
-- =====================================================================

CREATE OR REPLACE FUNCTION pg_temp.agregar_check(tabla text, nombre text, regla text) RETURNS void AS $$
BEGIN
    IF to_regclass(tabla) IS NULL
       OR EXISTS (SELECT 1 FROM pg_constraint WHERE conname = nombre AND conrelid = to_regclass(tabla)) THEN
        RETURN;
    END IF;
    BEGIN
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I CHECK (%s)', tabla, nombre, regla);
    EXCEPTION WHEN check_violation THEN
        -- Hay datos antiguos que no cumplen: la regla aplica a filas nuevas sin bloquear el arranque.
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I CHECK (%s) NOT VALID', tabla, nombre, regla);
        RAISE WARNING 'Restriccion % creada como NOT VALID: revise los datos existentes de %', nombre, tabla;
    END;
END;
$$ LANGUAGE plpgsql@@

-- ---------- Reglas de dominio (las mismas validaciones de los DTO) ----------
SELECT pg_temp.agregar_check('vehiculos', 'ck_vehiculos_anio_modelo', 'anio_modelo BETWEEN 1950 AND 2100')@@
SELECT pg_temp.agregar_check('vehiculos', 'ck_vehiculos_capacidad', 'capacidad_carga_kg > 0')@@
SELECT pg_temp.agregar_check('operaciones', 'ck_operaciones_periodo', 'fecha_fin >= fecha_inicio')@@
SELECT pg_temp.agregar_check('operaciones', 'ck_operaciones_vehiculos', 'numero_vehiculos_requeridos > 0')@@
SELECT pg_temp.agregar_check('operaciones', 'ck_operaciones_entregas', 'numero_entregas_esperadas > 0')@@
SELECT pg_temp.agregar_check('necesidades_cliente', 'ck_necesidades_horario', 'hora_fin > hora_inicio')@@
SELECT pg_temp.agregar_check('necesidades_cliente', 'ck_necesidades_cantidad', 'cantidad_vehiculos > 0')@@
SELECT pg_temp.agregar_check('necesidades_cliente', 'ck_necesidades_capacidad', 'capacidad_minima_kg > 0')@@
SELECT pg_temp.agregar_check('rutas', 'ck_rutas_distancia', 'distancia_estimada_km > 0')@@
SELECT pg_temp.agregar_check('rutas', 'ck_rutas_entregas', 'numero_entregas_programadas > 0')@@
SELECT pg_temp.agregar_check('rutas', 'ck_rutas_recorrido', 'lower(trim(punto_origen)) <> lower(trim(punto_destino))')@@
SELECT pg_temp.agregar_check('rutas', 'ck_rutas_fechas_reales', 'fecha_fin_real IS NULL OR fecha_inicio_real IS NULL OR fecha_fin_real >= fecha_inicio_real')@@
SELECT pg_temp.agregar_check('posiciones_ruta', 'ck_posiciones_latitud', 'latitud BETWEEN -90 AND 90')@@
SELECT pg_temp.agregar_check('posiciones_ruta', 'ck_posiciones_longitud', 'longitud BETWEEN -180 AND 180')@@
SELECT pg_temp.agregar_check('posiciones_ruta', 'ck_posiciones_velocidad', 'velocidad_kmh IS NULL OR velocidad_kmh >= 0')@@
SELECT pg_temp.agregar_check('entregas', 'ck_entregas_novedad', 'estado <> ''NO_ENTREGADA'' OR length(trim(coalesce(novedad, ''''))) > 0')@@
SELECT pg_temp.agregar_check('usuarios_app', 'ck_usuarios_correo', 'position(''@'' in correo) > 1')@@

-- ---------- Indices de llaves foraneas (PostgreSQL no los crea solo) ----------
CREATE INDEX IF NOT EXISTS idx_operaciones_cliente ON operaciones (cliente_id)@@
CREATE INDEX IF NOT EXISTS idx_necesidades_operacion ON necesidades_cliente (operacion_id)@@
CREATE INDEX IF NOT EXISTS idx_rutas_operacion ON rutas (operacion_id)@@
CREATE INDEX IF NOT EXISTS idx_rutas_zona ON rutas (zona_id)@@
CREATE INDEX IF NOT EXISTS idx_rutas_vehiculo ON rutas (vehiculo_id)@@
CREATE INDEX IF NOT EXISTS idx_rutas_conductor ON rutas (conductor_id)@@
CREATE INDEX IF NOT EXISTS idx_rutas_estado ON rutas (estado)@@
CREATE INDEX IF NOT EXISTS idx_entregas_ruta ON entregas (ruta_id)@@
-- El mapa consulta las posiciones mas recientes de cada ruta.
CREATE INDEX IF NOT EXISTS idx_posiciones_ruta_fecha ON posiciones_ruta (ruta_id, fecha_registro DESC)@@

-- ---------- Integridad entre filas ----------
-- Un vehiculo y un conductor no pueden estar en dos rutas EN_CURSO al mismo tiempo.
DO $$
BEGIN
    IF NOT EXISTS (SELECT vehiculo_id FROM rutas WHERE estado = 'EN_CURSO' AND vehiculo_id IS NOT NULL GROUP BY vehiculo_id HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS ux_rutas_vehiculo_en_curso ON rutas (vehiculo_id) WHERE estado = 'EN_CURSO';
    ELSE
        RAISE WARNING 'Hay vehiculos en varias rutas EN_CURSO; no se creo ux_rutas_vehiculo_en_curso';
    END IF;
    IF NOT EXISTS (SELECT conductor_id FROM rutas WHERE estado = 'EN_CURSO' AND conductor_id IS NOT NULL GROUP BY conductor_id HAVING count(*) > 1) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS ux_rutas_conductor_en_curso ON rutas (conductor_id) WHERE estado = 'EN_CURSO';
    ELSE
        RAISE WARNING 'Hay conductores en varias rutas EN_CURSO; no se creo ux_rutas_conductor_en_curso';
    END IF;
END
$$@@

-- Los dias de una necesidad le pertenecen: si se borra la necesidad, se borran sus dias.
DO $$
DECLARE
    fk text;
BEGIN
    SELECT c.conname INTO fk
    FROM pg_constraint c
    WHERE c.conrelid = to_regclass('necesidad_dias') AND c.contype = 'f' AND c.confdeltype <> 'c'
    LIMIT 1;
    IF fk IS NOT NULL THEN
        EXECUTE format('ALTER TABLE necesidad_dias DROP CONSTRAINT %I', fk);
        ALTER TABLE necesidad_dias ADD CONSTRAINT fk_necesidad_dias_necesidad
            FOREIGN KEY (necesidad_id) REFERENCES necesidades_cliente (id) ON DELETE CASCADE;
    END IF;
END
$$@@
