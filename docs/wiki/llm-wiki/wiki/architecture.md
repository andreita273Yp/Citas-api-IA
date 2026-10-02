# Arquitectura y límites

## HECHO

El backend usa Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, JPA, MySQL 8.4, Flyway, Spring Security/JWT y REST JSON. El frontend importado usa Angular 21, TypeScript y Node 24, sin Express/BFF.

## PREFERENCIA

Mantener especificaciones, contratos y evidencia cross-repo en la Wiki del backend, sin convertir la raíz en un tercer repositorio.

## DECISIÓN — 2026-09-24 · Ubicación documental

La documentación operativa se conserva en las rutas reales versionadas: `citas-api/docs/wiki/scrum/`, `citas-api/docs/wiki/llm-wiki/` y `citas-api/docs/wiki/subagents/`. Las referencias históricas a `docs/FCV Dev/` quedan obsoletas.

## DECISIÓN — 2026-10-01 · Estructura hexagonal por módulo

Cada módulo de `citas-api` sigue `domain` → `application` (`port/in`, `port/out`, servicios) → `adapter` (`in/web`, `out/persistence`, `out/security`) → `config`.

- `domain` y `application` no importan Spring, JPA, Servlet ni Hibernate; `FoundationIntegrationTest` lo verifica.
- Los casos de uso se ensamblan como beans en `<módulo>/config`. Las transacciones entran por el puerto `TransactionPort`, implementado con `TransactionTemplate`.
- La persistencia de identidad usa Spring Data JPA (`RESTRICCIONES_TECNICAS`).
- `shared/security` (cadena de filtros, CORS, JWT Bearer) y `shared/web` (Problem Details) son transversales.
- Todos los módulos cumplen esta estructura: `identity`, `catalog`, `offer`, `insurance` y `scheduling`.
  - Escriben con Spring Data JPA.
  - Los modelos de lectura SQL (`ProfessionalReadModel`, `AppointmentReadModel`, `OperationsReadModel`) son adaptadores de salida.
  - Ningún controlador contiene SQL (desde la Fase 6).
- `/api/v1/admin/**` exige ADMIN y `/api/v1/professional/**` exige PROFESSIONAL en `SecurityConfig`. Cada caso de uso agrega sus propias validaciones.
- **Red de verificación local (S3 · V8):** `.githooks/pre-commit` en cada repositorio, activado con `git config core.hooksPath .githooks`.
  - **Siempre:** bloquea archivos `.env` y patrones de secretos en las líneas agregadas.
  - **Si el commit toca código, o con `HOOK_FULL=1`:** ejecuta en Docker la suite backend (MySQL real) o, en el frontend, lint, Vitest y build.

## DECISIÓN — 2026-09-24 · Entorno de frontend

Docker monta el código de `citas-web` y un volumen Linux independiente en `/workspace/node_modules`. Las dependencias se instalan con `npm ci` dentro de `citas-web-dev`; no se reutilizan binarios nativos instalados en Windows.
