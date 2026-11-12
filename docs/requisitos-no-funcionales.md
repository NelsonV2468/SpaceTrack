# Requisitos no funcionales

> **Estado:** propuesta inicial preparada por QA para aprobacion del Product Owner. Los valores son objetivos de referencia para un centro de operaciones de una sola empresa; ajustelos si el negocio define otros.

| Estado | Significado |
|---|---|
| ✅ Implementado | El sistema ya lo cumple |
| 🟡 Parcial | Existe una parte; falta completar |
| ⛔ Pendiente | Aun no implementado |

## RNF-01 Concurrencia y carga

| Requisito | Objetivo | Estado |
|---|---|---|
| Usuarios simultaneos en el panel | 25 | ⛔ Sin prueba de carga |
| Rutas EN_CURSO simultaneas | 100 | ⛔ Sin prueba de carga |
| Entregas programadas por ruta | Maximo recomendado 200 | ⛔ Sin limite en la API |
| Operaciones concurrentes sobre la misma ruta (registrar entregas, iniciar) | Sin sobrepasar cupos ni duplicar recursos | ⛔ Requiere bloqueo optimista (`@Version`) o pesimista |

## RNF-02 Tiempos de respuesta

| Operacion | Objetivo (p95) | Estado |
|---|---|---|
| CRUD de cualquier modulo | < 500 ms | ⛔ Sin medicion |
| `GET /api/rutas/ubicaciones` con 100 rutas en curso | < 1 s | ⛔ Sin medicion |
| Refresco del mapa | Cada 5 s sin degradar el panel | ✅ |

## RNF-03 Retencion de datos

| Dato | Retencion | Estado |
|---|---|---|
| Posiciones de rutas (`posiciones_ruta`) | 30 dias, purga diaria configurable | ✅ |
| Rutas y entregas | Se conservan como historial; las rutas EN_CURSO o FINALIZADAS no se eliminan | ✅ |
| Datos personales de conductores y contactos | Mientras exista la relacion contractual mas el plazo legal que defina el area juridica | ⛔ |
| Copias de seguridad de la base | Diaria, 30 dias | ⛔ H2 en archivo local, sin respaldo automatico |

## RNF-04 Seguridad, roles y acceso

| Requisito | Estado |
|---|---|
| Autenticacion de usuarios (Spring Security) | ⛔ Hoy la aplicacion no exige credenciales |
| Roles: `ADMIN` (gestion completa), `OPERADOR` (rutas y entregas), `CONSULTA` (solo lectura) | ⛔ |
| Proteccion CSRF en formularios con sesion | ⛔ |
| Consola H2 deshabilitada fuera del perfil de desarrollo | ⛔ Hoy siempre habilitada |
| HTTPS en ambientes compartidos | ⛔ |
| Errores sin trazas ni detalles internos | ✅ |
| Escape de datos en pantallas (prevencion de XSS) | ✅ |

## RNF-05 Proteccion de datos personales (Ley 1581 de 2012 y Decreto 1377 de 2013)

SpaceTrack trata datos personales de conductores (documento, telefono, correo, licencia) y de contactos de clientes.

| Requisito | Estado |
|---|---|
| Politica de tratamiento de datos publicada y aceptada por los titulares | ⛔ |
| Registro de la autorizacion del titular (fecha y finalidad) | ⛔ |
| Acceso a datos personales restringido por rol (ver RNF-04) | ⛔ |
| Procedimiento para consultas, reclamos y supresion de datos del titular | ⛔ |
| Registro de auditoria de quien consulta o modifica datos personales | ⛔ |

## RNF-06 Usabilidad y accesibilidad

| Requisito | Estado |
|---|---|
| Interfaz en espanol con mensajes de error comprensibles | ✅ |
| Formularios con etiquetas asociadas y foco inicial en los modales | ✅ |
| Operacion completa con teclado, incluida la seleccion de rutas en el mapa (WCAG 2.1 AA) | 🟡 El mapa solo responde al clic |
| Estado de las paradas no transmitido solo con color | 🟡 Existe un texto emergente, pero no accesible por teclado |

## RNF-07 Mantenibilidad y operacion

| Requisito | Estado |
|---|---|
| Pruebas automatizadas de servicios (`mvn test`) | ✅ |
| Pruebas de controladores y repositorios | ⛔ |
| Migraciones de esquema versionadas (Flyway) en lugar de `ddl-auto=update` | ⛔ |
| Integracion continua que ejecute las pruebas en cada pull request | ⛔ |
| Hora del sistema fija en `America/Bogota` | ✅ |
