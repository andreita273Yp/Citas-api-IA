# Trazabilidad

## HECHO

Las sesiones S2-S6 requieren commits y evidencias específicas. El backend y frontend deben mantener historial trazable; las pruebas y la evidencia cross-repo son parte de la evaluación.

## HECHO — 2026-09-17

Las HU-001 a HU-036 existen en `docs/FCV Dev/scrum/`. HU-001/002/003 tienen avance parcial; HU-004 define por ahora solo el contrato de identidad. La implementación backend de HU-005/006/007 cuenta con `AuthIntegrationTest`, `IdentityTest` y `AuthRequestGuardTest`; la integración web se controla mediante HU-033.

## HECHO — 2026-09-22

El catálogo de ocho subagentes fue versionado en `docs/FCV Dev/subagents/` y enlazado desde el orquestador. `citas-web` contiene trabajo local React/Vite de autenticación y pruebas; no debe declararse completado hasta ejecutar build, typecheck, tests y verificación cross-repo.

## HECHO — 2026-10-01 · Paso 5 (Fase 5, S4-B)

HU-025 a HU-028 quedan `Completada`, y HU-002 también, con su CA-03 probado. Evidencia:

- Backend: `AppointmentLifecycleIntegrationTest` (11).
- Frontend: `my-appointments.spec.ts`; pantalla `MyAppointments` y decisión de reprogramaciones en `OperationsPanel`.

## HECHO — 2026-10-01 · Paso 4 (Fase 4, S4-A)

HU-008 a HU-013 quedan `Completada`. Evidencia:

- Backend: `PasswordRecoveryIntegrationTest` (6), `ProfileAndAffiliationIntegrationTest` (7) e `InsuranceCatalogAdministrationIntegrationTest` (5).
- Frontend: `identity-api.spec.ts`; pantallas `IdentityDashboard` y `AdminInsurance`.
- `IdentityExtensionIntegrationTest` se retiró: la reemplazan las suites anteriores, y su prueba de recuperación había sido el RED abierto desde el Paso 0.

## HECHO — 2026-10-01 · Paso 3 (Fase 3, S3-B)

HU-017 a HU-024 quedan `Completada`. Evidencia:

- Backend: `AgendaBlocksIntegrationTest` (9), `BookingIntegrationTest` (10), `SpecializedDecisionIntegrationTest` (5) y `SchedulingDomainTest` (7).
- Frontend: `booking-api.spec.ts` y `book-appointment.spec.ts`; pantallas `ProfessionalAgenda`, `BookAppointment` y la bandeja de `OperationsPanel`.
- Evidencia S3 consolidada en `docs/evidence/S3_EVIDENCE.md`. El hook de pre-commit y la demostración del secreto ficticio quedan para el Paso 7.

## HECHO — 2026-10-01 · Paso 2 (Fase 2, S3-A)

HU-014, HU-015 y HU-016 quedan `Completada`; HU-017 queda `En desarrollo` hasta probar su CA-03 con los bloques de HU-018.

- Evidencia: `SpecialtyAdministrationIntegrationTest` (4) y `ProfessionalAdministrationIntegrationTest` (7) sobre MySQL real; `offer-api.spec.ts`; pantalla `AdminOffer`.
- RED → GREEN: las 11 pruebas fallaron con `404` antes de existir los endpoints.

## HECHO — 2026-10-01 · Paso 1 (cierre S2)

HU-001, HU-003 a HU-007 quedan `Completada` con evidencia que apunta a pruebas existentes:

- `FoundationIntegrationTest`, `RegistrationIntegrationTest`, `LoginIntegrationTest`, `SessionLifecycleIntegrationTest`, `AuthRequestGuardIntegrationTest` y `CatalogIntegrationTest`, sobre MySQL real.
- `auth-api.spec.ts` y `auth.interceptor.spec.ts`.

Evidencia RED → GREEN: `RegistrationIntegrationTest.ca02_ca04_rejectsSameEmailIgnoringCaseAndSpacesWithoutCreatingAnotherAccount` y `LoginIntegrationTest.ca01_ca04_…` fallaron con 400 antes de mover la normalización del email al dominio.

HU-002 sigue `En desarrollo` hasta probar automáticamente la integridad de agenda (CA-03) en la Fase 3.

## HECHO — 2026-09-24 · Fase 1

Se ejecutó el corte S2 autorizado sin n8n ni reglas de agenda: Flyway V2 publica catálogos fijos, las rutas protegidas se cubren con `CatalogIntegrationTest` y el cliente Angular consume registro, login, refresh y logout. La verificación dentro de Docker registró backend `mvn test` (7 pruebas), frontend `npm run lint`, `ng test --watch=false` (5 pruebas) y `npm run build`, todos exitosos. Las HU-001/002 continúan parciales porque el modelo de oferta, disponibilidad y citas pertenece a fases aprobables posteriores.
