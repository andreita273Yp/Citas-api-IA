# Arquitectura

## HECHO — límites de repositorio

`citas-api` es el backend previsto con Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, Spring Data JPA, MySQL 8.4, Flyway, Spring Security y JWT access/refresh. `citas-web` será React o Angular con TypeScript y Node.js 24, elegido después del ciclo Stitch y Google AI Studio. Fuente: [Restricciones técnicas](../raw/approved/restricciones-tecnicas.md).

No existe Express ni BFF: el frontend consume directamente la API REST JSON de Spring Boot. La URL backend debe ser configurable por environment. Fuente: [PRD](../raw/approved/PRD-v1.0.md) y [Restricciones técnicas](../raw/approved/restricciones-tecnicas.md).

## Estado de implementación

No iniciado. Al crear esta wiki no hay aplicación Spring Boot ni frontend importado. No existe aún contrato REST. Ver [Contrato REST](rest-contract.md).

## Restricciones operativas

Solo `citas-api` y `citas-web` son repositorios Git. `main` representa estabilidad y `develop` trabajo. Las automatizaciones n8n se versionan como JSON bajo `citas-api/automations/n8n/`. Fuente: [Restricciones técnicas](../raw/approved/restricciones-tecnicas.md).
