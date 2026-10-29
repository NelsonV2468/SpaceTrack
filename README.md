# SpaceTrack - Gestion de clientes

Modulo de Gestion de SpaceTrack para CITY URBAN BOGOTA. Centraliza empresas contratantes, vehiculos, conductores y operaciones/contratos que despues se asociaran a las rutas.

## Tecnologias

- Java 21
- Spring Boot 3.4.4
- Spring MVC, Thymeleaf y API REST
- Spring Data JPA
- H2 en archivo local para desarrollo
- Maven y JUnit 5 / Mockito

## Arquitectura

El proyecto es un monolito por capas. Cada modulo de negocio se organiza en `domain`, `repository`, `service`, `dto` y `controller`; las excepciones comunes viven en `shared`.

```
Navegador / API REST -> Controller -> Service -> Repository -> H2
```

La interfaz web y la API usan el mismo servicio, con lo cual las reglas de NIT y correo unicos se aplican de igual forma en ambos canales.

## Requisitos cubiertos

- Crear, consultar, editar y eliminar empresas clientes.
- Validar campos obligatorios, NIT colombiano con o sin digito de verificacion, correo y telefono.
- Evitar NIT y correos de contacto duplicados.
- Marcar un cliente como activo o inactivo.
- Guardar fecha de creacion y actualizacion.
- Exponer API REST preparada para futuras integraciones.
- Registrar, editar, consultar y eliminar vehiculos de la flota.
- Registrar, editar, consultar y eliminar conductores con licencia vigente.
- Registrar contratos/operaciones asociados obligatoriamente a un cliente.
- Validar placa, documento, licencia y codigo de contrato como datos unicos.

## Ejecutar en IntelliJ IDEA

1. Abre la carpeta `SpaceTrack` como proyecto Maven.
2. Configura el SDK del proyecto como Java 21.
3. Espera la descarga de dependencias de Maven.
4. Ejecuta `SpaceTrackApplication`.
5. Abre `http://localhost:8080` (redirige al panel `panel.html`).

La consola H2 queda disponible en `http://localhost:8080/h2-console`. Usa la URL JDBC `jdbc:h2:file:./data/spacetrack`, usuario `sa` y contrasena vacia.

## API REST

| Metodo | Ruta | Funcion |
|---|---|---|
| GET | `/api/clientes` | Lista clientes |
| GET | `/api/clientes/{id}` | Consulta un cliente |
| POST | `/api/clientes` | Registra un cliente |
| PUT | `/api/clientes/{id}` | Actualiza un cliente |
| DELETE | `/api/clientes/{id}` | Elimina un cliente |

Las pantallas de gestion estan disponibles en `/clientes`, `/vehiculos`, `/conductores` y `/operaciones`.

| Metodo | Ruta | Funcion |
|---|---|---|
| GET, POST, PUT, DELETE | `/api/vehiculos` | Administra la flota |
| GET, POST, PUT, DELETE | `/api/conductores` | Administra conductores |
| GET, POST, PUT, DELETE | `/api/operaciones` | Administra contratos y operaciones |

| PATCH | `/api/rutas/{id}/asignacion` | Asigna vehiculo y conductor (`{"vehiculoId":1,"conductorId":1}`) |
| PATCH | `/api/rutas/{id}/iniciar`, `/api/rutas/{id}/finalizar` | Cambia el estado de la ruta |
| GET | `/api/rutas/{id}/ubicacion`, `/api/rutas/ubicaciones` | Ubicacion simulada del vehiculo |
| GET, POST, DELETE | `/api/entregas` (`?rutaId=`) | Registro de entregas realizadas |

## Panel de operacion

`/panel.html` es la interfaz principal: tablero de control, monitoreo en mapa, gestion, planeacion y registro de entregas.

- **Necesidades del cliente**: vehiculos, capacidad, cadena de frio, dias y horario por contrato.
- **Asignacion**: cada ruta recibe un vehiculo y un conductor. Se valida que el vehiculo este activo y tenga cadena de frio si el contrato la exige, que el conductor este activo con licencia vigente y que ninguno este en otra ruta en curso. Una ruta no se puede iniciar sin ambos.
- **Entregas programadas**: se definen por ruta y se comparan con las esperadas por el contrato.
- **Estado de la ruta**: PENDIENTE -> EN_CURSO -> FINALIZADA.
- **Entregas realizadas**: solo en rutas en curso, sin superar las programadas; las no entregadas exigen una novedad.
- **Mapa**: plano esquematico de Bogota en SVG (funciona sin internet). Las paradas se ubican en la localidad de la zona y el vehiculo avanza entre ellas segun las entregas registradas; se actualiza cada 5 segundos. No es GPS real.

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

## Subir al repositorio con un buen commit

Desde la carpeta `gerencia de software` abre la terminal de IntelliJ y ejecuta:

```powershell
git add SpaceTrack
git status
git commit -m "feat(clientes): implementar CRUD de empresas contratantes"
git branch -M main
git remote add origin URL_DE_TU_REPOSITORIO
git push -u origin main
```

Antes del commit, confirma que solo aparecen archivos de `SpaceTrack` y que no se incluyan `target`, `data` ni `.idea`; el `.gitignore` del modulo los excluye. Si `origin` ya existe, reemplaza el cuarto comando por `git remote set-url origin URL_DE_TU_REPOSITORIO`.
