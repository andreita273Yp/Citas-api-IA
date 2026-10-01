# Log de operaciones

| Fecha | Operación | Fuentes/páginas | Resultado |
|---|---|---|---|
| 2026-09-17 | INGEST inicial | SRC-PRD-001, SRC-TECH-001, SRC-DB-001, SRC-TRACE-001 | Estructura y páginas iniciales creadas tras aprobación del usuario |
| 2026-09-17 | DECISIÓN | HU-001 a HU-007; `contracts.md`, `decisions.md` | Contrato y corte backend de identidad aprobados; HU-033 conserva integración web |
| 2026-09-17 | LEARN/LINT | Flyway V1, pruebas MySQL 8.4; `data-integrity.md`, `traceability.md` | Corte 3FN y evidencia backend añadidos; RAW intacto, sin nuevos enlaces estructurales |
| 2026-09-22 | LEARN/DECISIÓN | `docs/FCV Dev/subagents/`, orquestador, `index.md`, `subagents.md`, arquitectura, decisiones, riesgos y trazabilidad | Ocho subagentes versionados; protocolo de delegación y nueva ubicación documental registrados; frontend React/Vite reconocido como trabajo pendiente de verificación |
| 2026-09-22 | LINT | Catálogo de subagentes y LLM Wiki | Enlaces Markdown relativos verificados; referencias operativas del orquestador actualizadas; se conserva como riesgo explícito la allowlist histórica de la Skill Scrum |
| 2026-09-24 | BASELINE/DECISIÓN | Docker, AGENTS, README, arquitectura, decisiones, riesgos e índice | Angular 21 y rutas reales de documentación reconciliadas; volumen Linux para dependencias frontend definido; decisiones mínimas del núcleo registradas; n8n diferido hasta completar S2-S4 |
| 2026-09-24 | IMPLEMENTACIÓN/VALIDACIÓN Fase 1 | Flyway V2, `CatalogIntegrationTest`, `AuthApi`, `AuthSession`, formularios Angular, contrato y trazabilidad | Catálogos fijos protegidos y acceso real cliente–API implementados. Backend 7/7, frontend lint/Vitest/build verdes en Docker; sin cambios de oferta, agenda o n8n. |
| 2026-10-01 | IMPLEMENTACIÓN/VALIDACIÓN Paso 0 | Flyway V6 + `db/seed`, controladores de citas/operaciones, catálogo JDBC, pruebas sobre MySQL, `decisions.md`, `contracts.md` | Persistencia alineada al modelo de referencia; zona `America/Bogota` fijada en la JVM. Backend 9/10 (falla `recoveryIsGenericAndTokenIsSingleUse` por endpoints de HU-008/009 aún inexistentes); prueba REST manual 28/28; frontend lint, 5/5 pruebas y build verdes. |
