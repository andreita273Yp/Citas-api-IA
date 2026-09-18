# Riesgos y preguntas abiertas

| Tipo | Hallazgo | Acción requerida |
| --- | --- | --- |
| Riesgo Git | Ambos repositorios están solo en `main`; no se observó `develop`. | Confirmar y crear/establecer flujo de trabajo antes de implementar. |
| Riesgo worktree | `.env.example` aparece eliminado en ambos repositorios. | Preservar el estado hasta instrucción expresa; no abrir ni restaurar por inferencia. |
| Pregunta de contrato | No hay especificación de endpoints, payloads, errores, fecha/hora, CORS o versionado. | Diseñar y aprobar contrato REST antes de integración. |
| Pregunta frontend | Aún no hay diseño aprobado ni elección React/Angular. | Completar Stitch → aprobación → AI Studio antes de estructurar UI. |
| Pregunta de dominio | Estados terminales y transiciones exactas de cita/reprogramación no están cerrados. | Registrar decisión antes de codificar máquinas de estado. |
| Pregunta de concurrencia | No se especifica mecanismo ni expiración de retenciones de slots. | Decidir modelo transaccional y pruebas de doble reserva. |
| Pregunta de catálogo | No se define la representación de Medicina General frente a especialidades configurables. | Acordar seed y semántica antes de API/datos. |
| Pregunta de identidad | Falta flujo inicial para credenciales de profesionales creados por ADMIN. | Definir procedimiento seguro. |
| Pregunta de datos | Falta decisión sobre historial de afiliación, snapshots y zona horaria. | Acordar antes de migraciones y contrato. |
| Riesgo de fuente | El modelo de referencia del trainer puede estar retenido intencionalmente. | No leer ni ingerir hasta autorización. |
| Pregunta n8n | Faltan contrato del webhook, autenticación, idempotencia y reintentos. | Resolver solo durante alcance S5/S6. |
