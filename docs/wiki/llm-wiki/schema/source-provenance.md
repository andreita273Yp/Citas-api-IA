# Procedencia de fuentes

Para cada fuente RAW registrar: nombre, versión o fecha visible, ubicación original, clasificación (`approved` o `context`), fecha de ingest y notas de alcance.

| Fuente RAW | Ubicación original | Clasificación | Alcance |
| --- | --- | --- | --- |
| `approved/PRD-v1.0.md` | `PRD.md` | approved | Requisitos funcionales y reglas de negocio. |
| `approved/restricciones-tecnicas.md` | `RESTRICCIONES_TECNICAS.md` | approved | Arquitectura, seguridad, Git y pruebas. |
| `approved/requisitos-normalizacion-3fn.md` | `database/REQUISITOS_NORMALIZACION_3FN.md` | approved | Requisitos de modelado y normalización. |
| `approved/evidencias-y-trazabilidad.md` | `EVIDENCIAS_Y_TRAZABILIDAD.md` | approved | Evidencia académica y Git. |
| `context/root-readme.md` | `README.md` | context | Contexto del laboratorio. |
| `context/citas-api-readme.md` | `citas-api/README.md` | context | Alcance inicial del backend. |
| `context/citas-web-readme.md` | `citas-web/README.md` | context | Alcance inicial del frontend. |

Fuentes excluidas por ahora: `database/reference/` hasta liberación del trainer, todo `.env` y las plantillas `AGENTS.md.template`.
