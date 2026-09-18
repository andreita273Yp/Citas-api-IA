# Automatización n8n

## HECHO — alcance diferido

S5/S6 contempla tres flujos: recordatorios de citas próximas, notificaciones ante cambios de estado y un resumen operativo diario opcional. Los workflows exportados deben ser JSON en `citas-api/automations/n8n/` y no contener credenciales. Fuente: [Evidencias](../raw/approved/evidencias-y-trazabilidad.md) y los documentos de workflow versionados en el repositorio.

## Estado

No iniciado. Los documentos existentes describen objetivos, no implementaciones ni contratos aprobados.

## PREGUNTAS ABIERTAS

- Payload y autenticación del webhook API → n8n.
- Idempotencia de recordatorios y notificaciones.
- Política de reintentos, trazabilidad y manejo de API no disponible.
