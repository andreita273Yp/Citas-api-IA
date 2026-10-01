# Trazabilidad

## HECHO

Las sesiones S2-S6 requieren commits y evidencias específicas. El backend y frontend deben mantener historial trazable; las pruebas y la evidencia cross-repo son parte de la evaluación.

## HECHO — 2026-09-17

Las HU-001 a HU-036 existen en `docs/FCV Dev/scrum/`. HU-001/002/003 tienen avance parcial; HU-004 define por ahora solo el contrato de identidad. La implementación backend de HU-005/006/007 cuenta con `AuthIntegrationTest`, `IdentityTest` y `AuthRequestGuardTest`; la integración web se controla mediante HU-033.

## HECHO — 2026-09-22

El catálogo de ocho subagentes fue versionado en `docs/FCV Dev/subagents/` y enlazado desde el orquestador. `citas-web` contiene trabajo local React/Vite de autenticación y pruebas; no debe declararse completado hasta ejecutar build, typecheck, tests y verificación cross-repo.

## HECHO — 2026-09-24 · Fase 1

Se ejecutó el corte S2 autorizado sin n8n ni reglas de agenda: Flyway V2 publica catálogos fijos, las rutas protegidas se cubren con `CatalogIntegrationTest` y el cliente Angular consume registro, login, refresh y logout. La verificación dentro de Docker registró backend `mvn test` (7 pruebas), frontend `npm run lint`, `ng test --watch=false` (5 pruebas) y `npm run build`, todos exitosos. Las HU-001/002 continúan parciales porque el modelo de oferta, disponibilidad y citas pertenece a fases aprobables posteriores.
