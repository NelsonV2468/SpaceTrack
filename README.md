# SpaceTrack - Gestion de clientes

Primer modulo de SpaceTrack para CITY URBAN BOGOTA. Registra y administra las empresas contratantes que despues se asociaran a operaciones, rutas, vehiculos y conductores.

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

## Ejecutar en IntelliJ IDEA

1. Abre la carpeta `SpaceTrack` como proyecto Maven.
2. Configura el SDK del proyecto como Java 21.
3. Espera la descarga de dependencias de Maven.
4. Ejecuta `SpaceTrackApplication`.
5. Abre `http://localhost:8080/clientes`.

La consola H2 queda disponible en `http://localhost:8080/h2-console`. Usa la URL JDBC `jdbc:h2:file:./data/spacetrack`, usuario `sa` y contrasena vacia.

## API REST

| Metodo | Ruta | Funcion |
|---|---|---|
| GET | `/api/clientes` | Lista clientes |
| GET | `/api/clientes/{id}` | Consulta un cliente |
| POST | `/api/clientes` | Registra un cliente |
| PUT | `/api/clientes/{id}` | Actualiza un cliente |
| DELETE | `/api/clientes/{id}` | Elimina un cliente |

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
