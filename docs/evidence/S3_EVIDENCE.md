# S3 EVIDENCE

GOAL: Implementar y verificar de extremo a extremo el incremento S3:

- ADMIN gestiona profesionales y asignaciones.
- PROFESSIONAL gestiona bloques de disponibilidad.
- USER consulta disponibilidad.
- Una cita general queda `APPROVED`; una especializada queda `REQUESTED` con sus slots retenidos.
- ADMIN aprueba, o rechaza con motivo y libera los slots.
- REST real entre Angular y Spring Boot, y una red de verificación que bloquea errores y secretos.

ITERACIONES_USADAS: 1/3 para el núcleo funcional (Pasos 2 y 3 del plan). V8 a V10 (hook) se completan en el Paso 7.

Entorno: Docker (`citas-api-dev`: Maven + Java 21; `citas-web-dev`: Node 24), MySQL 8.4 real. Las pruebas usan la base `<DB_NAME>_test`, que se recrea en cada ejecución. Fecha: 2026-10-01.

## RED

| Test | Comando | Resultado | Causa |
|---|---|---|---|
| `SpecialtyAdministrationIntegrationTest` (4) y `ProfessionalAdministrationIntegrationTest` (7) | `mvn -B test -Dtest=SpecialtyAdministrationIntegrationTest,ProfessionalAdministrationIntegrationTest` | 11/11 FAIL (`expected:<201\|403\|409> but was:<404>`) | No existían `/api/v1/admin/specialties` ni `/api/v1/admin/professionals` |
| `AgendaBlocksIntegrationTest` (9), `BookingIntegrationTest` (10) y `SpecializedDecisionIntegrationTest` (5) | `mvn -B test -Dtest=AgendaBlocksIntegrationTest,BookingIntegrationTest,SpecializedDecisionIntegrationTest` | 23/24 FAIL | No existían bloques ni decisión ADMIN. Además: reserva en el pasado aceptada (`expected:<400> but was:<200>`) y PROFESSIONAL podía reservar (`expected:<403> but was:<200>`) |

Todos los fallos son funcionales (estados HTTP y reglas de negocio). No hay errores de compilación ni dependencias rotas. Detalle completo en [red-green-log.md](red-green-log.md).

## GREEN

- **Comando:** `docker compose exec -T citas-api-dev mvn -B test`
- **Resultado:** `Tests run: 74, Failures: 1`. Las 35 pruebas S3 están en GREEN. La única falla, `IdentityExtensionIntegrationTest.recoveryIsGenericAndTokenIsSingleUse`, pertenece a HU-008/009 (S4) y es el RED de la Fase 4.
- **Prueba REST manual sobre la app en ejecución con los datos demo:** 56/56 verificaciones, sin errores en el log.

## SLOTS_30

- `SchedulingDomainTest.thirtyMinuteSpecialtyUsesEveryFreeSlot`: cada slot libre es un inicio de 30 min.
- `AgendaBlocksIntegrationTest.hu018_ca01_…`: un bloque de 08:00 a 12:00 publica 8 slots de 30 min.
- `SpecialtyAdministrationIntegrationTest.ca02_…`: con 30 min, un bloque de 2 h ofrece 4 inicios.

## SLOTS_60

- `SchedulingDomainTest.sixtyMinuteSpecialtyNeedsTwoConsecutiveFreeSlotsOfTheSameProfessionalAndLocation`.
- `BookingIntegrationTest.hu021_ca02_…`: un bloque de 08:00 a 10:00 ofrece 3 inicios de 60 min. Al ocupar 08:30 solo queda 09:00–10:00.
- `hu023_…`: una cita de 60 min retiene exactamente 2 slots (08:00 y 08:30).

## DOUBLE_BOOKING

- **Protección en persistencia** (`BookingService` + `ProfessionalSlotJpaRepository`), en tres capas:
  1. `SELECT ... FOR UPDATE` de los slots del rango, en orden de inicio.
  2. Verificación de que estén libres y sean consecutivos.
  3. `UPDATE ... SET appointment_id=? WHERE id IN (...) AND appointment_id IS NULL`, cuyo conteo debe coincidir; si no coincide, se revierte toda la transacción.
- `BookingIntegrationTest.hu022_ca02_concurrentBookingsOfTheSameSlotProduceExactlyOneAppointment`: 8 pacientes reservan el mismo slot a la vez; resultado: exactamente una respuesta `201`, el resto `409`, 1 cita y 1 slot ocupado. Repetida 5 veces: 5/5 estable.
- `hu022_ca02_aSlotTakenBetweenSearchAndConfirmationIsRejected`: un slot tomado entre la búsqueda y la confirmación devuelve 409 y no crea una segunda cita.
- `hu023_…`: un slot retenido por una solicitud `REQUESTED` no se puede reservar (409).

## AUTHORIZATION

- **USER:**
  - No administra profesionales ni especialidades (`hu015_ca02_…`, `ca01_rejectsDuplicatesAndNonAdminActors`: 403).
  - No resuelve solicitudes (`SpecializedDecisionIntegrationTest.ca03_nonAdminActorsCannotDecide`: 403).
  - No publica bloques (`onlyProfessionalsManageBlocks`: 403).
- **PROFESSIONAL:**
  - Gestiona solo sus bloques: uno ajeno devuelve 404 (`hu019_ca03_…`) y solo ve los suyos (`hu020_…`).
  - No resuelve solicitudes (403).
  - No reserva como paciente (`onlyUsersBookTheirOwnAppointments`: 403).
- **ADMIN:** gestiona profesionales y especialidades, y aprueba o rechaza solicitudes (200).
- **Ownership:** el historial y el detalle de una cita solo los ve su dueño; otro USER recibe 404.
- **Sin token:** 401 en todas las rutas protegidas.
- Regla central en `SecurityConfig`: `/api/v1/admin/**` → ADMIN y `/api/v1/professional/**` → PROFESSIONAL.

## GENERAL_APPOINTMENT

- `hu022_ca01_ca03_generalAppointmentIsApprovedAutomaticallyAndAudited`: `201`, `status = APPROVED`, slot ocupado, `approved_at` registrado y sin intervención ADMIN.

## SPECIALIZED_APPOINTMENT

- `hu023_ca01_ca02_ca03_specializedRequestIsRequestedHoldsBothSlotsAndIsAudited`: `201`, `status = REQUESTED`, 2 slots retenidos.

## ADMIN_APPROVE

- `SpecializedDecisionIntegrationTest.ca01_approvingKeepsTheReservationAndIsAuditedAsAdmin`: REQUESTED → `APPROVED`, conserva los 2 slots, registra `approved_by` y `approved_at`, y la bandeja queda vacía.

## ADMIN_REJECT

- **Sin motivo:** `ca03_rejectionWithoutReasonIsRefusedWithoutTransition` devuelve `400` (también con motivo en blanco o decisión inválida). El estado sigue `REQUESTED` y los slots siguen retenidos.
- **Con motivo** (`ca02_rejectingWithReasonRecordsItAndReleasesTheSlots`):
  - Resultado `REJECTED`.
  - El motivo "Sin orden médica de remisión" queda en el historial y en `decisionReason` de la cita.
  - **Slots liberados:** 0 slots asociados a la cita; el horario vuelve a ofrecerse y se puede reservar de nuevo.
- **Solicitud ya decidida:** `409` (`ca03_onlyRequestedAppointmentsCanBeDecided`).

## PAST / WRONG LOCATION / WRONG SPECIALTY

- **Bloque pasado:** `400` (`hu018_ca02_…`).
- **Cita en el pasado:** `400` (`bookingRejectsPastMisalignedAndWrongLocationOrSpecialty`).
- **Sede no asignada:**
  - Bloque: `409` (`hu018_ca02_…`).
  - Reserva: `409`.
- **Profesional inactivo publicando:** `409` (`hu017_ca03_…`).
- **Especialidad no asociada:** `409`.
- **Especialidad inactiva:** `404` en reserva y búsqueda (`hu021_ca03_…`, `SpecialtyAdministrationIntegrationTest.ca03_…`).

## AUDIT

- `appointment_status_history` es append-only: `AppointmentStatusHistory` solo inserta y lee, y no existe endpoint de modificación (RN-12).
- **Creación general:** `APPROVED`, fuente `SYSTEM`.
- **Creación especializada:** `REQUESTED`, fuente `USER` (actor = paciente).
- **Aprobación:** `REQUESTED → APPROVED`, fuente `ADMIN`.
- **Rechazo:** `REQUESTED → REJECTED`, fuente `ADMIN`, con motivo.
- `GET /appointments/{id}/history` devuelve el estado anterior, el nuevo, el actor, la fuente, el motivo y la fecha.
- Las transiciones son explícitas (`AppointmentStatus.transitionTo`, `SchedulingDomainTest.appointmentTransitionsAreExplicitAndTerminalStatesAreFinal`).

## FRONTEND

- **build:** `docker compose exec citas-web-dev npm run build` → PASS (`Application bundle generation complete`).
- **typecheck:** incluido en `ng build` y `ng test` (compilador Angular/TypeScript estricto) → PASS. `npm run lint` → `All files pass linting`.
- **tests:** `npx ng test --watch=false` → 6 archivos, 22/22 PASS:
  - `auth-api`, `auth.interceptor`, `offer-api`, `booking-api`, `book-appointment` y `app`.
  - `book-appointment` verifica que la UI muestra el estado decidido por el backend y maneja 409/400.
- **REST real, sin mocks en estos flujos:**
  - `AdminOffer`: especialidades y profesionales.
  - `ProfessionalAgenda`: bloques.
  - `BookAppointment`: búsqueda y reserva.
  - `OperationsPanel`: aprobar o rechazar.

## HOOK_FAIL

- Pendiente: Paso 7. Se demostrará con `FAKE_SECRET_FOR_S3_TEST_ONLY` contra un hook de pre-commit versionado.

## HOOK_PASS

- Pendiente: Paso 7.

## COMMITS

- **citas-api (develop):**
  - `9f544f8`: feat(s3), administrar especialidades y profesionales.
  - El commit del Paso 3 es el que agrega este archivo.
- **citas-web (develop):**
  - `4339c3d`: feat(s3), pantallas ADMIN.
  - El commit del Paso 3 agrega `BookAppointment`, `ProfessionalAgenda` y la decisión en la bandeja.

## PENDIENTES

Solo funcionalidades fuera del alcance de S3:

- Recuperación de contraseña, perfil y afiliación, y CRUD de EPS y planes (HU-008 a HU-013, S4).
- Mis citas, cancelación y reprogramación (HU-025 a HU-028, S4).
- Agenda del profesional y cierre de atención (HU-029/030), y bandeja unificada e historial con filtros (HU-031/032, S4).
- Hook de pre-commit y la demostración con `FAKE_SECRET_FOR_S3_TEST_ONLY` (V8 a V10, Paso 7).
