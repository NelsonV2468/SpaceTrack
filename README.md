# SpaceTrack

Sistema de gestion de rutas de distribucion para **City Urban Bogota**. Centraliza clientes, contratos (operaciones), necesidades del cliente, flota, conductores, zonas, rutas, entregas y el monitoreo simulado de los vehiculos en un plano de Bogota.

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
- Tipo y cantidad de vehiculos, capacidad minima, cadena de frio, dias y franja horaria por contrato.
- La suma de vehiculos de las necesidades no supera los vehiculos del contrato.
- **Los dias de una necesidad deben estar dentro de los dias de operacion del contrato.**

### Zonas
- Codigo y nombre unicos.
- **La localidad debe pertenecer al catalogo** (las 19 localidades urbanas de Bogota mas Soacha, Mosquera y Funza), para que la zona siempre se pueda ubicar en el mapa. Se acepta sin tildes ni mayusculas y se guarda con su nombre oficial. El catalogo se consulta en `GET /api/zonas/localidades`.

### Rutas
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
| GET, POST | `/api/necesidades` (`?operacionId=`) | Lista o registra necesidades del cliente |
| GET, PUT, DELETE | `/api/necesidades/{id}` | Consulta, actualiza o elimina una necesidad |
| GET, POST | `/api/zonas` | Lista o registra zonas |
| GET | `/api/zonas/localidades` | Catalogo de localidades con sus coordenadas |
| GET, PUT, DELETE | `/api/zonas/{id}` | Consulta, actualiza o elimina una zona |
| GET, POST | `/api/rutas` (`?estado=PENDIENTE\|EN_CURSO\|FINALIZADA`) | Lista o registra rutas |
| GET, PUT, DELETE | `/api/rutas/{id}` | Consulta, actualiza o elimina una ruta |
| PATCH | `/api/rutas/{id}/asignacion` | Asigna vehiculo y conductor (`{"vehiculoId":1,"conductorId":1}`) |
| PATCH | `/api/rutas/{id}/iniciar` | Inicia la ruta |
| PATCH | `/api/rutas/{id}/finalizar` | Finaliza la ruta; puede devolver `advertencia` |
| GET | `/api/rutas/{id}/ubicacion`, `/api/rutas/ubicaciones` | Ubicacion simulada del vehiculo |
| GET, POST | `/api/entregas` (`?rutaId=`) | Lista o registra entregas |
| DELETE | `/api/entregas/{id}` | Corrige (elimina) una entrega |
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

## Requisitos no funcionales

Los objetivos de concurrencia, tiempos de respuesta, retencion de datos, roles y cumplimiento de la Ley 1581 de 2012 estan en [docs/requisitos-no-funcionales.md](docs/requisitos-no-funcionales.md), con el estado de implementacion de cada uno.

## Contribuir

1. Crea una rama desde `main` con el prefijo del tipo de cambio y el ticket, por ejemplo `feat/ON-20-descripcion` o `fix/ON-21-descripcion`.
2. Ejecuta `mvn test` antes de cada commit.
3. Usa commits convencionales en espanol, por ejemplo `feat(rutas): validar horario del contrato al iniciar`.
4. Abre un pull request hacia `main`.

No se versionan `target/`, `data/` ni `.idea/` (ver `.gitignore`).
