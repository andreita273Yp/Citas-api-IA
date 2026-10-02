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

## Oferta de atención — `/api/v1/admin` (HU-014 a HU-017)

### DECISIÓN — 2026-10-01

Todas las rutas `/api/v1/admin/**` exigen rol ADMIN en `SecurityConfig`: sin token responden `401`, con otro rol `403`. Los errores usan Problem Details: `400` datos inválidos, `404` recurso inexistente, `409` duplicado.

| Operación | Entrada | Éxito |
|---|---|---|
| `GET /specialties` | — | `200`, lista con `id`, `code`, `name`, `durationMinutes`, `general`, `requiresAdminApproval`, `active` (incluye inactivas) |
| `POST /specialties` | `code`, `name`, `durationMinutes` (30\|60) | `201`. Se crea especializada (`general=false`, `requiresAdminApproval=true`); el código se normaliza a MAYÚSCULAS_CON_GUIONES |
| `PATCH /specialties/{id}` | `name?`, `durationMinutes?`, `active?` | `200`. No existe `DELETE` (`405`): retirar es desactivar |
| `GET /professionals` · `GET /professionals/{id}` | — | `200`: `id`, `userId`, nombres, `email`, `phone`, `professionalCode`, `licenseNumber`, `active`, `specialties[{id,code,name,durationMinutes,primary}]`, `locations[{id,code,name}]` |
| `POST /professionals` | `firstName`, `lastName`, `documentType`, `documentNumber`, `email`, `phone`, `initialPassword`, `professionalCode`, `licenseNumber` | `201`. Crea la identidad PROFESSIONAL y el profesional en una sola transacción; `initialPassword` se guarda en BCrypt y nunca se devuelve |
| `PATCH /professionals/{id}` | `active` | `200` |
| `PUT /professionals/{id}/specialties` | `assignments[{specialtyId, primary}]` | `200`. Reemplaza el conjunto: una o más especialidades activas y exactamente una primaria. Las que salen quedan `active=false` (se conserva el historial) |
| `PUT /professionals/{id}/locations` | `locationIds[]` | `200`. Solo sedes fijas activas (HIC=1, ICV=2), sin repetidos |

## Mis citas, cancelación y reprogramación (HU-025 a HU-028)

### DECISIÓN — 2026-10-01

Sustituye las rutas homónimas del ajuste V6. Los ids ahora son numéricos, no texto.

| Operación | Rol | Entrada | Éxito / errores |
|---|---|---|---|
| `GET /api/v1/appointments` | autenticado (propias) | `status?`, `from?`, `to?` | `200`; cada cita incluye `id`, `status`, `locationId`, `locationCode`, `location`, `professionalId`, `professional`, `specialtyId`, `specialty`, `durationMinutes`, `startsAt`, `endsAt`, `reason`, `decisionReason` (solo si está rechazada) y `reschedule` (última solicitud o `null`). Estado desconocido o rango inválido → `400` |
| `GET /api/v1/appointments/{id}` | dueño | — | `200`; una cita ajena o inexistente → `404` |
| `POST /api/v1/appointments/{id}/cancel` | dueño | — | `200` cita `CANCELLED`. Libera todos sus slots y cancela su reprogramación pendiente. Pasada o terminal → `409` |
| `POST /api/v1/appointments/{id}/reschedule-requests` | dueño | `startAt`, `locationId?` | `201` `{id, appointmentId, status=PENDING, locationId, locationCode, startAt, endAt, decisionReason, patientAction}`. Mantiene profesional, especialidad y duración. No aprobada o pasada, ya pendiente, franja ocupada o sede no ofrecida → `409`; fuera de cuadrícula o en el pasado → `400` |
| `POST /api/v1/appointments/{id}/reschedule-requests/{requestId}/keep` | dueño | — | `204`; tras un rechazo registra `KEEP_APPOINTMENT`. Si no está rechazada o ya eligió → `409`. Cancelar tras un rechazo registra `CANCEL_APPOINTMENT` |
| `POST /api/v1/admin/reschedule-requests/{id}/decision` | ADMIN | `decision` = `APPROVE` \| `REJECT`, `reason` (obligatorio en `REJECT`) | `200` con la solicitud. Aprobar mueve la cita y libera la franja anterior; rechazar libera la nueva. Ya decidida → `409`; inexistente → `404` |

## Identidad ampliada y aseguramiento (HU-008 a HU-013)

### DECISIÓN — 2026-10-01

| Operación | Rol | Entrada | Éxito / errores |
|---|---|---|---|
| `POST /api/v1/auth/password-recovery` | público | `email` | Siempre `202` sin cuerpo, exista o no la cuenta |
| `POST /api/v1/auth/password-reset` | público | `token`, `password`, `confirmation` | `204`; revoca todas las sesiones refresh. Confirmación distinta o contraseña débil → `400`; token inválido, vencido o usado → `401` |
| `GET /api/v1/admin/local-mailbox/password-recovery` | ADMIN | — | `200` `[{email, token, expiresAt, createdAt}]`, del más reciente al más antiguo. Solo existe con `app.recovery.local-mailbox=true` (desarrollo); si no, `404` |
| `GET /api/v1/users/me` | autenticado | — | `200` `{id, firstName, lastName, documentType, documentNumber, email, phone, roles}` |
| `PATCH /api/v1/users/me` | autenticado | `phone` (otros campos se ignoran) | `200` perfil; teléfono inválido (7 a 15 dígitos, `+` opcional) → `400` |
| `GET /api/v1/users/me/affiliation` | autenticado | — | `200` `{planId, planName, epsId, epsName, regimeCode, regimeName, membershipNumber, validFrom}` o `204` sin afiliación |
| `PUT /api/v1/users/me/affiliation` | autenticado | `planId`, `membershipNumber` | `200`. El régimen y la EPS salen del plan: ya no se envía `regimeCode`. Plan inexistente, inactivo o de EPS inactiva → `400` |
| `DELETE /api/v1/users/me/affiliation` | autenticado | — | `204`; termina la vigente |
| `GET /api/v1/catalogs/eps` · `GET /api/v1/catalogs/eps-plans?epsId` | público | — | Solo EPS activas y planes activos de EPS activas; el plan incluye `regimeId/regimeCode/regimeName` |
| `GET/POST /api/v1/admin/eps` · `PATCH /api/v1/admin/eps/{id}` | ADMIN | `code`, `name` / `name?`, `active?` | `201`/`200`; duplicado → `409`; no hay `DELETE` (`405`) |
| `GET/POST /api/v1/admin/eps-plans` · `PATCH /api/v1/admin/eps-plans/{id}` | ADMIN | `epsId`, `regimeId`, `code`, `name` / `name?`, `regimeId?`, `active?` | `201`/`200`; EPS o régimen inexistente → `400`; código repetido en la EPS → `409`; no hay `DELETE` |
| `GET /api/v1/catalogs/regimes` (y demás catálogos fijos) | autenticado | — | Cada elemento incluye ahora `id`, además de `code` y `name` |

## Agenda, disponibilidad y reserva (HU-018 a HU-024)

### DECISIÓN — 2026-10-01

Formatos: fecha `YYYY-MM-DD`, hora `HH:mm`, inicio de cita `YYYY-MM-DDTHH:mm`, en la zona `America/Bogota`. Todo se alinea a la cuadrícula de 30 minutos.

| Operación | Rol | Entrada | Éxito / errores |
|---|---|---|---|
| `GET /api/v1/professional/me` | PROFESSIONAL | — | `200` perfil propio con sedes y especialidades; un usuario sin registro profesional → `403` |
| `GET /api/v1/professional/blocks` | PROFESSIONAL | `from?`, `to?`, `locationId?` | `200` bloques propios: `id`, `locationId`, `locationCode`, `date`, `startTime`, `endTime`, `slots`, `bookedSlots` |
| `POST /api/v1/professional/blocks` | PROFESSIONAL | `locationId`, `date`, `startTime`, `endTime` | `201`. Pasado, fuera de cuadrícula o fin ≤ inicio → `400`; profesional inactivo, sede no asignada o solape → `409` |
| `PUT /api/v1/professional/blocks/{id}` | PROFESSIONAL | igual que POST | `200`, regenera los slots. Bloque ajeno → `404`; ya iniciado o con citas comprometidas → `409` |
| `DELETE /api/v1/professional/blocks/{id}` | PROFESSIONAL | — | `204`; mismas restricciones que PUT |
| `GET /api/v1/catalogs/specialties` | autenticado | `type?` = `GENERAL` \| `SPECIALIZED` | `200` especialidades activas con `durationMinutes`, `general`, `requiresAdminApproval` |
| `GET /api/v1/catalogs/professionals` | autenticado | `specialtyId`, `locationId` | `200` profesionales que ofrecen esa especialidad en esa sede |
| `GET /api/v1/availability` | autenticado | `specialtyId`, `date`, `locationId?`, `professionalId?` | `200` inicios reservables: `professionalId`, `professionalName`, `locationId`, `locationCode`, `startAt`, `endAt`, `durationMinutes`. Especialidad inactiva → `404`. Nunca devuelve inicios pasados; 60 min exige 2 slots libres consecutivos |
| `POST /api/v1/appointments` | USER | `professionalId`, `locationId`, `specialtyId`, `startAt`, `reason?` | `201` (antes `200`) con `id`, `status` (`APPROVED` para Medicina General, `REQUESTED` para el resto), `startAt`, `endAt`, `durationMinutes`. Pasado o fuera de cuadrícula → `400`; otro rol → `403`; especialidad inactiva → `404`; no ofrecida o franja ocupada/retenida → `409` |
| `POST /api/v1/admin/appointments/{id}/decision` | ADMIN | `decision` = `APPROVE` \| `REJECT`, `reason` (obligatorio en `REJECT`) | `200` `{id, status}`. Decisión inválida o rechazo sin motivo → `400`; inexistente → `404`; estado distinto de `REQUESTED` → `409` |

Doble reserva (RN-01): `POST /appointments` bloquea los slots del rango con `SELECT ... FOR UPDATE`, verifica que estén libres y sean consecutivos, y los asigna con un `UPDATE ... WHERE appointment_id IS NULL` cuyo conteo debe coincidir; si no coincide, se revierte la transacción. Un interbloqueo bajo concurrencia responde `409` reintentable.

Efectos sobre la reserva (RN-07 y RN-08): `GET /catalogs/professionals`, `GET /availability` y `POST /appointments` solo aceptan un profesional activo, con la especialidad activa asociada y la sede asignada. Una especialidad inactiva o inexistente responde `404` y una combinación no ofrecida `409`. La duración siempre sale del catálogo.

### Impacto cross-repo antes del cambio REST

- `citas-api`: nuevo `pom.xml`, código de dominio/aplicación/adaptadores, migración Flyway, configuración, pruebas y este contrato.
- `citas-web`: `AuthApi` consume registro, login, refresh y logout con credenciales; `AuthSession` mantiene el access token en memoria. El formulario de acceso deja de seleccionar roles simulados.
- Compatibilidad: `/api/v1` fija la versión de este contrato; cambios posteriores requieren revisión de ambas partes. Migraciones: identidad V1 y catálogos fijos V2. Pruebas: REST, seguridad, persistencia y `mvn test` en backend; lint, Vitest y build en frontend; recorrido manual local de las pantallas de acceso.

## PREGUNTA ABIERTA

Las rutas, filtros, paginación y formatos de fecha/hora de las demás HU siguen sin contrato aprobado.
