# Arquitectura y límites

## HECHO

El backend usa Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, JPA, MySQL 8.4, Flyway, Spring Security/JWT y REST JSON. El frontend importado usa Angular 21, TypeScript y Node 24, sin Express/BFF.

## PREFERENCIA

Mantener especificaciones, contratos y evidencia cross-repo en la Wiki del backend, sin convertir la raíz en un tercer repositorio.

## DECISIÓN — 2026-09-24 · Ubicación documental

La documentación operativa se conserva en las rutas reales versionadas: `citas-api/docs/wiki/scrum/`, `citas-api/docs/wiki/llm-wiki/` y `citas-api/docs/wiki/subagents/`. Las referencias históricas a `docs/FCV Dev/` quedan obsoletas.

## DECISIÓN — 2026-09-24 · Entorno de frontend

Docker monta el código de `citas-web` y un volumen Linux independiente en `/workspace/node_modules`. Las dependencias se instalan con `npm ci` dentro de `citas-web-dev`; no se reutilizan binarios nativos instalados en Windows.
