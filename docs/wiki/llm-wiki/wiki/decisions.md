# Registro de decisiones

## DECISIÓN — 2026-09-17

La raíz no se convierte en un tercer repositorio. La única LLM Wiki se versiona dentro de `citas-api/docs/wiki/llm-wiki/`.

## DECISIÓN — 2026-09-17 · Identidad backend

El usuario aprobó el incremento mínimo de HU-001/002/004, el seed parcial de HU-003 y el backend completo de HU-005/006/007. Email se compara sin distinguir mayúsculas y documento por tipo+número. El refresh va en cookie HttpOnly para sitios distintos y rota en cada uso. Logout revoca el refresh de esa sesión. Las obligaciones de interfaz se trasladan a HU-033.

## DECISIÓN — 2026-09-24 · Contrato base del núcleo de citas

- La zona de negocio es `America/Bogota`.
- El contrato REST representa una fecha con `YYYY-MM-DD` y una hora con `HH:mm`; nunca deriva la franja de la zona del navegador.
- Las citas usan los estados `REQUESTED`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` y `NO_SHOW`. Las transiciones se implementarán de manera explícita y auditada en la fase correspondiente.
- Las reprogramaciones usan `PENDING`, `APPROVED` y `REJECTED`; mientras están en `PENDING`, la cita y su franja original permanecen vigentes.
- La clasificación de una especialidad será un atributo de catálogo `GENERAL` o `SPECIALIZED`; la especialidad de Medicina General será el único catálogo inicialmente marcado `GENERAL`.
- La persistencia almacenará instantes en UTC cuando corresponda, mientras el contrato y las validaciones de agenda se interpretan en `America/Bogota`.
- La toma de slots se resolverá en una única transacción con bloqueo pesimista ordenado de los slots solicitados, comprobación de disponibilidad y una relación única cita-slot. La validación previa de disponibilidad no será la única protección.

## DECISIÓN — 2026-10-01 · Persistencia alineada al modelo 3FN de referencia (Paso 0)

Sustituye, donde contradiga, a la decisión del 2026-09-24.

- La persistencia adopta el modelo de referencia del trainer (`database/reference/db.sql`) mediante Flyway `V6__reference_3fn_model.sql`. V1-V5 no se modifican; V6 retira sus tablas provisionales, que nunca se aplicaron en un entorno compartido. El PRD §7 y el README permiten usar el modelo de referencia tras la actividad de normalización.
- Flyway queda habilitado. `db/migration` contiene esquema y catálogos fijos o públicos (roles, regímenes, estados, sedes, especialidades); `db/seed/R__demo_seed.sql` contiene los datos sintéticos de demostración y solo se carga en local (`FLYWAY_LOCATIONS`).
- Identificadores numéricos (`BIGINT`/`SMALLINT`). El contrato los expone como texto en las vistas de citas para no romper el cliente.
- Las fechas de citas, bloques y slots se guardan como `DATETIME` en hora local de `America/Bogota`; la JVM fija esa zona al iniciar (`CitasApiApplication.BUSINESS_ZONE`). Deja sin efecto el almacenamiento en UTC.
- Medicina General se identifica con `specialties.is_general = TRUE` y `requires_admin_approval = FALSE`; no se crea un atributo `GENERAL`/`SPECIALIZED` aparte.
- Una reprogramación `PENDING` retiene su nueva franja asignando esos `professional_slots` a la misma cita; aprobar libera la franja anterior y rechazar libera solo la nueva (RN-10). No se agrega tabla de retenciones.
- El motivo de rechazo de una cita se toma del `appointment_status_history` de su estado `REJECTED`; la tabla `appointments` del modelo no tiene esa columna.
- `change_source` admite solo `SYSTEM`, `USER` y `ADMIN` (RF-19). El cierre de atención de un PROFESSIONAL se registra como `USER`, con el actor en `changed_by_user_id`.
- Las pruebas de integración usan MySQL real en `<DB_NAME>_test` (creada por `database/init/01-test-database.sh`), recreada con Flyway clean+migrate en cada ejecución, en lugar de H2.

## DECISIÓN — 2026-10-01 · Operación profesional y administrativa (Fase 6)

- **Agenda:** la vista `WEEK` va de lunes a domingo de la fecha dada. Del paciente solo se expone el nombre (RF-16); nunca el documento, el correo ni el teléfono.
- **Cierre:** solo después de `scheduled_end_at` según el reloj de negocio. Al cerrar, una reprogramación pendiente pierde sentido: se cancela y se libera su franja retenida.
- **Bandeja:** los filtros de sede y fecha de una reprogramación se aplican a la franja solicitada, porque es la que ADMIN debe decidir. La franja vigente se muestra aparte (`currentStartsAt`).
- **Inmutabilidad del historial (RN-12):** se garantiza en la aplicación. El adaptador solo inserta, no hay endpoints de escritura, y una prueba (`hu032_ca02_productionCodeOnlyAppendsToTheHistory`) falla si el código de producción hace UPDATE o DELETE sobre la tabla o la mapea como entidad JPA.
- **Trigger descartado:** se evaluó un trigger MySQL que rechazara UPDATE y DELETE. Con binlog activo, crearlo exige `SUPER` o `log_bin_trust_function_creators=1`, así que la migración haría fallar el arranque en un MySQL estándar.
- **Estado anterior:** se deriva de la entrada previa del historial, porque el modelo V6 no tiene esa columna.
- `OperationsController` se reemplazó por `adapter/in/web/OperationsController` sobre `OperationsService`. Ya ningún controlador contiene SQL.

## DECISIÓN — 2026-10-01 · Ciclo de vida de la cita (Fase 5)

- **Cancelar** aplica a citas propias `REQUESTED` o `APPROVED` cuyo inicio es futuro. Libera todos los slots de la cita, incluida una franja retenida por reprogramación, y marca esa solicitud como `CANCELLED`.
- **Reprogramar** conserva profesional, especialidad y duración; cambiar de profesional es una cita nueva. Admite otra sede solo si el profesional la tiene asignada y ofrece ahí la especialidad. Hay a lo sumo una solicitud `PENDING` por cita.
- **Tras un rechazo** (RF-15), el USER elige conservar la cita (`/keep` → `KEEP_APPOINTMENT`) o cancelarla, lo que registra `CANCEL_APPOINTMENT` en `patient_action_after_rejection`.
- **Auditoría:** la aprobación de una reprogramación se registra en el historial como `APPROVED` (fuente ADMIN, "Reprogramación aprobada"). La solicitud y el rechazo no cambian el estado de la cita: su traza queda en `reschedule_requests`.
- `AppointmentLifecycleController` se reemplazó por `MyAppointmentsController` sobre `AppointmentLifecycleService`. Solo `OperationsController` (Fase 6) conserva SQL en el controlador.

## DECISIÓN — 2026-10-01 · Identidad ampliada y aseguramiento (Fase 4)

- **Recuperación de contraseña:**
  - El token es aleatorio de 256 bits (base64url), vence en 30 minutos y se usa una sola vez. Solo se persiste su hash SHA-256, y una solicitud nueva invalida las anteriores.
  - Restablecer revoca todas las sesiones refresh del usuario.
- **Entrega del token:** como no hay SMTP (RF-03 lo permite), se usa un buzón local en memoria que solo lee ADMIN y solo existe con `RECOVERY_LOCAL_MAILBOX=true`, el valor por defecto en local. **En cualquier entorno compartido debe ponerse en `false`**: entonces el token no se entrega y solo se registra el evento, sin datos.
- **Perfil:** solo el teléfono es editable.
- **Afiliación:** referencia solo al plan; EPS y régimen se derivan de él (RF-04). Hay a lo sumo una afiliación vigente por usuario, y volver a elegir un plan reactiva su fila en lugar de duplicarla. La afiliación es opcional y no condiciona búsqueda ni reserva.
- **EPS y planes:** se desactivan en lugar de borrarse. Un plan exige un régimen explícito. Desactivar una EPS saca sus planes del catálogo de selección sin tocar las afiliaciones existentes.
- `IdentityExtensionController` se reemplazó por `ProfileController` (identity) e `InsuranceController` (insurance).

## DECISIÓN — 2026-10-01 · Agenda y reserva (Fase 3)

- "Ahora" sale de un `Clock` de negocio en `America/Bogota`. RN-06 rechaza bloques que ya empezaron y citas cuyo inicio no es futuro, y la disponibilidad nunca ofrece inicios pasados.
- Un profesional no puede tener bloques solapados el mismo día, aunque sean en sedes distintas: no puede estar en dos lugares a la vez. Bloques adyacentes (12:00–14:00 y 14:00–16:00) son válidos y sus slots se encadenan para citas de 60 min.
- Editar o eliminar un bloque solo se permite si es futuro y ninguno de sus slots tiene `appointment_id`. Eliminar borra el bloque y sus slots, porque no tienen historial ni citas asociadas.
- Solo un actor con rol USER reserva, y siempre para sí mismo. PROFESSIONAL y ADMIN sin rol USER reciben `403`.
- Aprobar exige que la cita aún no haya empezado. Rechazar libera los slots en la misma transacción.
- El módulo `scheduling` sigue la estructura hexagonal. `AppointmentLifecycleController` y `OperationsController` (Fases 5 y 6) siguen con SQL en controladores hasta esas fases.

## DECISIÓN — 2026-10-01 · Oferta de atención (Fase 2)

- Las especialidades que crea ADMIN son siempre especializadas: requieren aprobación (RN-03). `is_general` solo lo tiene Medicina General, por seed.
- Para el alta de un PROFESSIONAL, ADMIN define una contraseña inicial con la misma política del registro (8 caracteres a 72 bytes). Se guarda en BCrypt y no se devuelve. El cambio por el propio profesional queda para la recuperación de contraseña (HU-008/009).
- La identidad PROFESSIONAL la crea el módulo `identity` (`CreateStaffAccountUseCase`) dentro de la misma transacción que el registro en `professionals`. Si algo falla, no se persiste nada.
- Las asignaciones de especialidades y sedes se reemplazan como conjunto con bloqueo pesimista de la fila del profesional. Las relaciones retiradas se marcan `active=false` en lugar de borrarse.
- Desactivar un profesional o una especialidad no modifica las citas existentes. Solo impide ofrecerlos y crear nuevas reservas (HU-017: el PRD no define efecto retroactivo).

## DECISIÓN — 2026-09-22 · Catálogo de subagentes

Los ocho subagentes especializados se mantienen como archivos Markdown versionados en `docs/wiki/subagents/`. El orquestador selecciona el perfil más específico, separa implementación de verificación y conserva la responsabilidad de coordinar cambios cross-repo y actualizar la Wiki.

## HECHO — 2026-09-24 · Frontend

Angular 21 es el framework detectado en `citas-web`; deja de ser una pregunta abierta. La prueba enfocada de `AuthApi` y lint se verificaron en un contenedor Linux. La aprobación visual y la integración funcional de HU-033 continúan pendientes.
