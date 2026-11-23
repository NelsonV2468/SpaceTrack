# SpaceTrack

Sistema de gestion de rutas de distribucion para **City Urban Bogota**. Centraliza clientes, contratos (operaciones), necesidades del cliente, flota, conductores, rutas por localidad, entregas y el monitoreo simulado de los vehiculos en un plano de Bogota.

## Tecnologias

- Java 21
- Spring Boot 3.4.4 (Spring MVC, Thymeleaf, API REST, Spring Data JPA, Bean Validation, Spring Security)
- Frontend en Next.js 16 + React 19 + TypeScript (carpeta `frontend/`), exportado como sitio estatico y servido por Spring
- Login con Neon Auth y roles (ADMIN, OPERADOR, CONSULTA)
- PostgreSQL (Neon) en produccion; despliegue en Render con Docker
- H2 en archivo local para desarrollo
- Maven y JUnit 5 / Mockito

## Arquitectura

Monolito por capas. Cada modulo de negocio (`clientes`, `vehiculos`, `conductores`, `operaciones`, `necesidades`, `zonas`, `rutas`, `entregas`, `posiciones`, `usuarios`) se organiza en `domain`, `repository`, `service`, `dto` y `controller`. Lo comun (excepciones, reloj, seguridad, configuracion) vive en `shared`.

```
Panel Next.js / pantallas Thymeleaf / API REST -> Spring Security -> Controller -> Service -> Repository -> H2 (dev) o PostgreSQL (prod)
```

Todas las interfaces usan los mismos servicios, por lo que las reglas de negocio se aplican igual en cada canal. Fechas y horarios se evaluan siempre en hora de Bogota (`America/Bogota`), sin importar la zona horaria del servidor.

## Ejecutar

### IntelliJ IDEA

1. Abre la carpeta `SpaceTrack` como proyecto Maven.
2. Configura el SDK del proyecto como Java 21.
3. Espera la descarga de dependencias de Maven.
4. Compila el frontend una vez (necesita Node.js 20 o superior):
   ```bash
   cd frontend
   npm install
   npm run build
   ```
   El build exporta el sitio y lo copia a `src/main/resources/webapp` (ignorado por git).
5. Ejecuta `SpaceTrackApplication` y abre `http://localhost:8080` (redirige a `/panel/`).

## Frontend (Next.js)

```
frontend/src/
  app/(acceso)/login, registro   Paginas separadas de inicio de sesion y creacion de cuenta
  app/panel/...                  Tablero, monitoreo, modulos, detalle de ruta, entregas, usuarios
  modulos/config.tsx             Columnas y campos de cada modulo CRUD (agregar uno nuevo = declararlo aqui)
  components/                    Formulario generico, modal, mapa SVG de Bogota, avisos, UI
  lib/                           Cliente del API (sesion + CSRF), Neon Auth, tipos y formatos
```

- **Desarrollo con recarga en caliente**: con el backend corriendo en `:8080` (perfil `dev`), ejecuta `npm run dev` en `frontend/` y abre `http://localhost:3000`. Next reenvia `/api` y `/auth` al backend.
- **Produccion**: el `Dockerfile` compila el frontend y lo empaqueta en el mismo `.jar`; todo queda en un solo servicio y dominio (la cookie de sesion y el CSRF funcionan sin CORS).
- **Seguridad**: la CSP no permite `'unsafe-inline'`; el build calcula los hashes SHA-256 de los scripts en linea de Next (`csp-hashes.txt`) y Spring autoriza solo esos.
- `npm run typecheck` valida los tipos de TypeScript.

### Linea de comandos

```bash
mvn spring-boot:run
```

```bash
mvn test
```

En el perfil `dev` (por defecto) la consola H2 queda disponible en `http://localhost:8080/h2-console` (URL JDBC `jdbc:h2:file:./data/spacetrack`, usuario `sa`, contrasena vacia) y el login esta apagado. El perfil `prod` usa PostgreSQL, exige login y deshabilita la consola H2; sus credenciales llegan por variables de entorno (ver `.env.example`).

### Perfil demo (datos de ejemplo)

Para presentar el sistema sin registrar todo a mano, arranca con el perfil `demo`:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Con el `.jar` empaquetado: `java -jar target/spacetrack-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo`. En IntelliJ IDEA: *Run/Debug Configurations* → *Active profiles* = `demo`.

Al arrancar se carga el caso de ejemplo del acta (distribucion con cadena de frio, flota de varios vehiculos y entregas zonificadas):

| Datos | Contenido |
|---|---|
| Cliente | Lacteos La Pradera SAS (demo), ficticio |
| Operacion | `CT-DEMO-001`, refrigerada, todos los dias, 5 vehiculos, 1.200 entregas, vigente desde hace 30 dias y por 6 meses |
| Necesidad | 5 furgones refrigerados de 1.500 kg; franja de 06:00 a 20:00, ampliada si la demo arranca de madrugada o de noche para que las rutas puedan iniciar |
| Flota | 5 furgones refrigerados (`DMO101` a `DMO105`) y 5 conductores con licencia vigente |
| Zonas | Las rutas usan las localidades de Chapinero, Kennedy, Suba, Engativa y Usaquen |
| Rutas | `R-DEMO-01` en curso y al dia · `R-DEMO-02` en curso y atrasada (genera la alerta critica) · `R-DEMO-03` en curso con 9 de 10 entregas y su vehiculo reportado fuera de ruta (genera la alerta de desvio) · `R-DEMO-04` pendiente · `R-DEMO-05` finalizada |

- Todo se crea con los servicios de cada modulo, asi que se aplican las mismas reglas de negocio que en el panel; la carga va en una sola transaccion y si una regla la rechaza no queda nada a medias.
- Solo con `demo` la aplicacion usa una base H2 en memoria: cada arranque empieza limpio y los datos se pierden al detenerla.
- Para conservarlos combina perfiles: `-Dspring-boot.run.profiles=dev,demo` usa la base de `./data`. Si ya hay clientes registrados la carga no se repite.
- La carga nunca impide que la aplicacion arranque. Si falla se reintenta una vez, por ejemplo si el dia cambia a medianoche durante la carga. Si vuelve a fallar, el registro explica el motivo (por ejemplo, una placa `DMO101` ya registrada) y la aplicacion sigue funcionando sin datos de ejemplo.
- Sin el perfil `demo` no se carga nada. No lo actives en produccion.

## Interfaces

- **Panel de operacion** (`/panel/`, responsive): tablero de control, monitoreo en mapa, gestion, planeacion, registro de entregas y, para administradores, usuarios y roles. El acceso se hace en `/login/` y las cuentas nuevas se crean en `/registro/`. Las direcciones antiguas `/panel.html` y `/login.html` redirigen a las nuevas.
- **Pantallas de gestion** (Thymeleaf): `/clientes`, `/vehiculos`, `/conductores` y `/operaciones`.
- **Mapa**: plano esquematico de Bogota en SVG (funciona sin internet). Las paradas se ubican en la localidad de la zona y el vehiculo avanza entre ellas segun las entregas registradas; se actualiza cada 5 segundos. Solo se dibujan vehiculos de rutas EN_CURSO. No es GPS real.

## Reglas de negocio

### Clientes
- NIT colombiano (6 a 10 digitos, con o sin digito de verificacion), correo y telefono validos.
- NIT y correo de contacto unicos.
- **Un cliente inactivo no puede tener contratos vigentes**: no se le registran operaciones ACTIVAS ni PAUSADAS, ni se crean o inician rutas de sus operaciones. Sus operaciones si se pueden cerrar (FINALIZADA).

### Operaciones (contratos)
- Asociadas obligatoriamente a un cliente; codigo de contrato unico (sin distinguir mayusculas ni espacios).
- **Estados**: `ACTIVA` y `PAUSADA` se pueden alternar (pausar y reactivar). `FINALIZADA` cierra el contrato y no se puede reabrir. No se puede finalizar una operacion con rutas EN_CURSO.
- **Vigencia**: una operacion abierta (ACTIVA o PAUSADA) no puede tener la fecha final en el pasado; se amplia la vigencia o se marca FINALIZADA. Una operacion vencida si se puede finalizar.
- **Dias de operacion**: texto validado que el sistema interpreta, por ejemplo `Lunes a viernes`, `Lunes, miercoles y viernes`, `De viernes a lunes` o `Todos los dias`.
- Las entregas esperadas no pueden quedar por debajo de las ya programadas en sus rutas, ni los vehiculos requeridos por debajo de los ya pedidos en sus necesidades.

### Necesidades del cliente
- **Se registran dentro de Operaciones, en el mismo formulario del contrato** (no hay pantalla aparte). El formulario tiene tres bloques: *Contrato*, *Cuando se opera* (dias y horario) y *Vehiculos que necesita el cliente* (tipo, cantidad, capacidad y cadena de frio). Cada dato se pide una sola vez: los dias, la cantidad de vehiculos y la cadena de frio valen para el contrato y para su necesidad.
- Se guarda todo en un paso (`/api/operaciones/planes`) y en una sola transaccion. La tabla de Operaciones muestra dias, horario, vehiculos y cuantos hay disponibles en la flota.
- Las necesidades adicionales registradas antes de este cambio se conservan y se suman al contrato.
- Tipo y cantidad de vehiculos, capacidad minima, cadena de frio, dias y franja horaria por contrato.
- La suma de vehiculos de las necesidades no supera los vehiculos del contrato.
- **Los dias de una necesidad deben estar dentro de los dias de operacion del contrato.**

### Zonas
- **Las zonas ya no se registran a mano**: son las 19 localidades de Bogota (codigos `LOC-01` Usaquen a `LOC-19` Ciudad Bolivar). Al arrancar, la aplicacion las guarda en la base si faltan (es idempotente) y el formulario de rutas solo ofrece elegir una en una lista desplegable.
- Las zonas registradas antes de este cambio se conservan para no romper sus rutas, pero se marcan con `catalogo: false` y no se ofrecen en rutas nuevas.

### Rutas
- **Registro guiado**: en el formulario se elige la operacion (contrato) y la zona; el resto se completa solo con los datos del contrato: codigo sugerido, nombre, destino y entregas programadas (las que le corresponden a un vehiculo, sin superar el cupo disponible). Se muestra un resumen del contrato (cliente, vigencia, dias, cadena de frio y cupo). Todo se puede ajustar antes de guardar. El usuario no indica la distancia: el sistema la calcula internamente desde el centro de distribucion hasta la localidad (en el API `distanciaEstimadaKm` es opcional). En Operaciones, el boton **Crear ruta** abre el formulario con el contrato ya elegido.
- Ciclo de vida: `PENDIENTE -> EN_CURSO -> FINALIZADA`. Solo se eliminan rutas PENDIENTES y no se editan rutas FINALIZADAS.
- **Crear una ruta** exige que la operacion este ACTIVA, que su cliente este activo y que hoy este dentro de la vigencia (`fechaInicio` a `fechaFin`).
- **Cupo de entregas**: la suma de entregas programadas de todas las rutas de un contrato no puede superar las entregas esperadas por el contrato.
- **Asignacion**: el vehiculo debe estar activo y tener cadena de frio si el contrato la exige; el conductor debe estar activo con licencia vigente; ninguno puede estar en otra ruta en curso. **Se permite reasignar recursos en una ruta EN_CURSO** (por ejemplo, ante una averia); el cambio queda en el log.
- **Iniciar una ruta** exige vehiculo y conductor asignados y revalida las condiciones de creacion, ademas de que:
  - hoy sea uno de los dias de operacion del contrato;
  - si el contrato tiene necesidades registradas, alguna opere hoy y la hora actual este dentro de su franja horaria.
- **Finalizar con entregas faltantes** esta permitido, pero la respuesta incluye una `advertencia` con la zona afectada y cuantas entregas quedaron sin registrar o no entregadas. El panel la muestra como alerta.

### Entregas
- Solo en rutas EN_CURSO y sin superar las programadas. Las no entregadas exigen una novedad.
- Solo se corrigen (eliminan) entregas de rutas EN_CURSO.

### Cierre diario de rutas (ON-20, epica ON-3 Seguimiento de rutas)
- **Pantalla** `/panel/cierres/` (menu Ejecucion > Cierre diario): se elige la fecha y se ve el resumen del dia (rutas finalizadas ese dia segun su hora real de fin, entregas realizadas, no entregadas y sin registrar, y cumplimiento) y el historial de cierres.
- **Cerrar el dia** (ADMIN u OPERADOR) guarda una copia fija del resumen con fecha, usuario y observaciones. No se permite si quedan rutas EN_CURSO iniciadas ese dia o antes, si la fecha es futura o si el dia ya esta cerrado.
- **Despues del cierre** no se pueden iniciar ni finalizar rutas en ese dia. Los cambios posteriores (por ejemplo, eliminar una ruta pendiente) no alteran el cierre guardado.

### Alertas de rutas proximas a finalizar (ON-4)
- Se evaluan las rutas EN_CURSO que tienen entregas pendientes. El fin de la ruta es el cierre de la franja horaria de hoy definida por las necesidades del contrato (la mas tardia entre las que ya empezaron).
- **Advertencia**: faltan `spacetrack.alertas.minutos-aviso` minutos o menos (60 por defecto) para el cierre de la franja.
- **Critica**: el tiempo restante no alcanza para las entregas pendientes (estimadas en `spacetrack.alertas.minutos-por-entrega` minutos cada una, 10 por defecto), la franja ya termino, o la ruta sigue abierta desde un dia anterior.
- Si el contrato no tiene necesidades que operen hoy, no hay franja y no se genera alerta.
- **Pantalla de alertas** (`/panel/alertas/`, menu Ejecucion > Alertas): indicadores de alertas criticas, advertencias y entregas en riesgo, tabla filtrable por nivel con enlace al detalle de cada ruta. El menu muestra un contador (rojo si hay criticas).
- El tablero del panel muestra las alertas vigentes (criticas primero, con enlace a la pantalla) y el detalle de la ruta muestra la suya; todo se actualiza cada 30 segundos.

### Alerta de desvio de ruta
- Si el vehiculo de una ruta EN_CURSO reporta su posicion (`POST /api/posiciones`, por GPS o registro manual), se compara con el recorrido planeado (centro de distribucion, paradas y destino).
- **Advertencia**: el vehiculo esta a mas de `spacetrack.alertas.desvio-km` (0,5 km) del recorrido. **Critica**: a mas de `spacetrack.alertas.desvio-critico-km` (2 km).
- El reporte cuenta durante `spacetrack.posiciones.vigencia-reporte-minutos` (30 por defecto; todo el dia en el perfil demo). Mientras este vigente el mapa muestra la posicion reportada (no la simulada) y no se generan posiciones simuladas para esa ruta.
- Se muestra en la pantalla de alertas (filtro *Vehiculos fuera de ruta*), el tablero, el detalle de la ruta y el monitoreo (aviso rojo, etiqueta *Fuera de ruta* y el vehiculo resaltado en el mapa).

### Posiciones
- Se genera una posicion simulada de cada ruta EN_CURSO cada `spacetrack.simulacion.intervalo-segundos` (10 por defecto).
- Se aceptan posiciones manuales por API solo para rutas existentes y EN_CURSO.
- Las consultas devuelven como maximo 1000 posiciones (`limite`, 100 por defecto).
- Las posiciones con mas de `spacetrack.posiciones.retencion-dias` (30 por defecto) se eliminan cada madrugada.

## API REST

Todas las respuestas de error tienen la forma `{"fecha", "estado", "mensaje", "errores"?}`. Las validaciones de formato responden 400; las reglas de negocio, 409; los recursos inexistentes, 404.

Con el login activo (perfil `prod`): todos los roles pueden consultar (GET); ADMIN y OPERADOR pueden crear y actualizar; solo ADMIN elimina y gestiona usuarios. Las peticiones que modifican datos requieren el token CSRF de la sesion.

| Metodo | Ruta | Funcion |
|---|---|---|
| GET, POST | `/api/clientes` | Lista o registra clientes |
| GET, PUT, DELETE | `/api/clientes/{id}` | Consulta, actualiza o elimina un cliente |
| GET, POST | `/api/vehiculos` | Lista o registra vehiculos |
| GET, PUT, DELETE | `/api/vehiculos/{id}` | Consulta, actualiza o elimina un vehiculo |
| GET, POST | `/api/conductores` | Lista o registra conductores |
| GET, PUT, DELETE | `/api/conductores/{id}` | Consulta, actualiza o elimina un conductor |
| GET, POST | `/api/operaciones` | Lista o registra contratos |
| GET, PUT, DELETE | `/api/operaciones/{id}` | Consulta, actualiza o elimina un contrato |
| GET, POST | `/api/operaciones/planes` | Lista o registra un contrato con su necesidad (formulario unico de Operaciones) |
| GET, PUT, DELETE | `/api/operaciones/planes/{id}` | Consulta, actualiza o elimina un contrato con su necesidad |
| GET, POST | `/api/necesidades` (`?operacionId=`) | Lista o registra necesidades del cliente |
| GET, PUT, DELETE | `/api/necesidades/{id}` | Consulta, actualiza o elimina una necesidad |
| GET | `/api/zonas` | Lista las zonas (localidades de Bogota) |
| GET | `/api/zonas/localidades` | Catalogo de localidades con sus coordenadas |
| GET | `/api/zonas/{id}` | Consulta una zona |
| GET, POST | `/api/rutas` (`?estado=PENDIENTE\|EN_CURSO\|FINALIZADA`) | Lista o registra rutas |
| GET, PUT, DELETE | `/api/rutas/{id}` | Consulta, actualiza o elimina una ruta |
| PATCH | `/api/rutas/{id}/asignacion` | Asigna vehiculo y conductor (`{"vehiculoId":1,"conductorId":1}`) |
| PATCH | `/api/rutas/{id}/iniciar` | Inicia la ruta |
| PATCH | `/api/rutas/{id}/finalizar` | Finaliza la ruta; puede devolver `advertencia` |
| GET | `/api/rutas/{id}/ubicacion`, `/api/rutas/ubicaciones` | Ubicacion simulada del vehiculo |
| GET, POST | `/api/entregas` (`?rutaId=`) | Lista o registra entregas |
| DELETE | `/api/entregas/{id}` | Corrige (elimina) una entrega |
| GET | `/api/alertas/rutas` | Alertas vigentes: franja horaria (`FRANJA_HORARIA`) y desvio de ruta (`DESVIO_RUTA`) |
| GET | `/api/alertas/rutas/{rutaId}` | Alertas de una ruta (lista vacia si no tiene) |
| GET | `/api/cierres` | Historial de cierres diarios |
| GET | `/api/cierres/{fecha}` | Cierre guardado de la fecha (`2026-11-16`), o la vista previa si el dia sigue abierto |
| POST | `/api/cierres` | Cierra un dia (`{"fecha":"2026-11-16","observaciones":"..."}`; solo ADMIN u OPERADOR) |
| GET | `/api/posiciones` (`?rutaId=&limite=`) | Posiciones guardadas, las mas recientes primero |
| GET | `/api/usuarios` | Lista usuarios (solo ADMIN) |
| PATCH | `/api/usuarios/{id}/rol` | Cambia el rol de un usuario (solo ADMIN) |
| GET | `/auth/config`, `/auth/me` | Configuracion de Neon Auth y usuario de la sesion actual |
| POST | `/auth/session` | Abre la sesion con el token de Neon Auth |
| POST | `/api/posiciones` | Registra una posicion manual (`{"rutaId":1,"latitud":4.65,"longitud":-74.08,"velocidadKmh":30}`; `velocidadKmh` es opcional) |

Ejemplo para crear un cliente:

```json
{
  "razonSocial": "Alimentos Urbanos SAS",
  "nit": "900123456-7",
  "nombreContacto": "Laura Gomez",
  "correoContacto": "operaciones@urbanos.co",
  "telefonoContacto": "+57 300 123 4567",
  "direccion": "Calle 10 # 20-30, Bogota",
  "activo": true
}
```

## Configuracion

| Propiedad | Valor por defecto | Uso |
|---|---|---|
| `spacetrack.simulacion.intervalo-segundos` | `10` | Frecuencia del generador de posiciones simuladas |
| `spacetrack.posiciones.retencion-dias` | `30` | Dias que se conservan las posiciones |
| `spacetrack.posiciones.purga-cron` | `0 30 2 * * *` | Hora de la purga diaria (hora de Bogota) |
| `spacetrack.alertas.minutos-aviso` | `60` | Minutos antes del cierre de la franja en que se advierte |
| `spacetrack.alertas.minutos-por-entrega` | `10` | Minutos estimados por entrega pendiente para decidir si una alerta es critica |

## Requisitos no funcionales

Los objetivos de concurrencia, tiempos de respuesta, retencion de datos, roles y cumplimiento de la Ley 1581 de 2012 estan en [docs/requisitos-no-funcionales.md](docs/requisitos-no-funcionales.md), con el estado de implementacion de cada uno.

## Contribuir

1. Crea una rama desde `main` con el prefijo del tipo de cambio y el ticket, por ejemplo `feat/ON-20-descripcion` o `fix/ON-21-descripcion`.
2. Ejecuta `mvn test` antes de cada commit.
3. Usa commits convencionales en espanol, por ejemplo `feat(rutas): validar horario del contrato al iniciar`.
4. Abre un pull request hacia `main`.

No se versionan `target/`, `data/` ni `.idea/` (ver `.gitignore`).
