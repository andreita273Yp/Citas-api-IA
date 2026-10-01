# Contratos REST

## HECHO

El PRD exige REST/JSON entre `citas-web` y `citas-api` y validación cross-repo en funcionalidades clave.

## DECISIÓN — 2026-09-24 · HU-003/004/005/006/007

El contrato inicial cubre solamente autenticación bajo `/api/v1/auth`:

| Operación | Entrada | Éxito |
|---|---|---|
| `POST /register` | JSON `firstName`, `lastName`, `documentType`, `documentNumber`, `email`, `phone`, `password` | `201`, JSON con `id`, datos públicos y rol `USER`, sin contraseña |
| `POST /login` | JSON `email`, `password` | `200`, JSON `accessToken`, `tokenType=Bearer`, `expiresIn`; cookie `refresh_token` |
| `POST /refresh` | Cookie `refresh_token` | `200`, nuevo access en JSON y nueva cookie refresh; la anterior se revoca |
| `POST /logout` | Cookie `refresh_token` | `204`, revocación de la sesión y cookie borrada |

Email se normaliza con trim y minúsculas. Documento es único por `(documentType, documentNumber)` normalizados. La contraseña de registro es obligatoria y se limita a 72 bytes UTF-8 por el límite de BCrypt; sus espacios no se alteran. Solo se permite autoregistro `USER`. Errores: `400` validación, `409` duplicidad, `401` credencial/refresh inválido, `403` rol insuficiente, en formato Problem Details; login no revela qué credencial falló.

Access JWT y refresh JWT usan secretos distintos, tipo explícito y duraciones configurables (valores iniciales: 15 minutos y 7 días). El access lleva `sub` y roles. El refresh lleva `sub` y `jti`; su identificador se guarda solo como hash en una sesión persistida. Un refresh válido rota ambos tokens atómicamente. Logout revoca solo la sesión indicada; un access emitido conserva validez hasta su expiración.

Para sitios distintos, la cookie es `HttpOnly; Secure; SameSite=None`, con `Path=/api/v1/auth`. CORS permite credenciales únicamente al `FRONTEND_ORIGIN` configurado. Login, refresh y logout requieren `Origin` permitido cuando se envía y `X-Requested-With: XMLHttpRequest`; el perfil HTTP local usa cookie `SameSite=Lax` sin `Secure`. `citas-web` envía ese encabezado y credenciales, conserva el access token solamente en memoria y lo elimina al cerrar sesión.

## Catálogos fijos — `/api/v1/catalogs`

Las siguientes rutas son de solo lectura, requieren un access JWT válido y devuelven una lista JSON. Una petición sin token responde `401`; cualquier método distinto de `GET` no está soportado (`405`). No existen rutas de escritura en este corte.

| Operación | Respuesta por elemento |
|---|---|
| `GET /roles` | `code`, `name` |
| `GET /appointment-statuses` | `code`, `name` |
| `GET /reschedule-statuses` | `code`, `name` |
| `GET /regimes` | `code`, `name` |
| `GET /locations` | `code`, `name`, `address` |

Los valores se originan exclusivamente en Flyway V2: roles `USER`, `PROFESSIONAL`, `ADMIN`; estados de cita y reprogramación del MVP; regímenes y las sedes sintéticas `HIC` e `ICV`. El cliente no debe usar estos catálogos como fuente de verdad local.

### DECISIÓN — 2026-10-01 · Ajuste por modelo de referencia V6

- Los catálogos fijos se originan en Flyway V6. `GET /locations` devuelve `id`, `code`, `name`, `address`, `city`, `department`; la dirección ya no incluye ciudad ni departamento.
- Regímenes: `CONTRIBUTIVO`, `SUBSIDIADO`, `ESPECIAL`, `EXCEPCION`, `PARTICULAR`. Estados de reprogramación: se agrega `CANCELLED`, que se aplica cuando el USER cancela una cita con reprogramación pendiente.
- En citas, agenda, bandeja e historial, `id`, `appointmentId`, `requestId` y `locationId` son identificadores numéricos serializados como texto (`"1"`), no códigos de sede. `POST /appointments/{id}/reschedule-requests` recibe `locationId` con ese mismo identificador.
- `RescheduleView` ya no incluye `reason`, porque el modelo no guarda motivo de la solicitud del USER; conserva `decisionReason`.
- `/error` es público para que un fallo interno responda `500` y no se oculte como `401`.
- Impacto en `citas-web`: `AppointmentApi` y `OperationsApi` ya tipan esos campos como `string`. La adopción visual se verifica en las fases de frontend.

### DECISIÓN — 2026-10-01 · Cierre S2 (HU-001 a HU-007)

- Registro y login normalizan el email (trim y minúsculas) **antes** de validarlo. Los errores de negocio responden Problem Details: `400` datos inválidos, `409` duplicado, `401` credencial o refresh inválido.
- `GET /actuator/health` es público y devuelve solo `{"status":"UP"}`; los demás endpoints de actuator no se exponen.
- CORS admite únicamente `FRONTEND_ORIGIN`; se eliminó el origen `http://localhost:4200` fijo en código.
- Cliente: `authInterceptor` agrega `X-Requested-With` y el Bearer a las llamadas a `citas-api`. Ante un `401` renueva una sola vez (compartida entre peticiones concurrentes) y reintenta; si el refresh falla, cierra la sesión. Al iniciar, intenta restaurar la sesión con la cookie refresh.

### Impacto cross-repo antes del cambio REST

- `citas-api`: nuevo `pom.xml`, código de dominio/aplicación/adaptadores, migración Flyway, configuración, pruebas y este contrato.
- `citas-web`: `AuthApi` consume registro, login, refresh y logout con credenciales; `AuthSession` mantiene el access token en memoria. El formulario de acceso deja de seleccionar roles simulados.
- Compatibilidad: `/api/v1` fija la versión de este contrato; cambios posteriores requieren revisión de ambas partes. Migraciones: identidad V1 y catálogos fijos V2. Pruebas: REST, seguridad, persistencia y `mvn test` en backend; lint, Vitest y build en frontend; recorrido manual local de las pantallas de acceso.

## PREGUNTA ABIERTA

Las rutas, filtros, paginación y formatos de fecha/hora de las demás HU siguen sin contrato aprobado.
