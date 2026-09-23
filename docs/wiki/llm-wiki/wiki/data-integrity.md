# Datos e integridad

## HECHO

El diseño debe alcanzar 3FN, resolver relaciones N:M con tablas puente, evitar duplicación de catálogos y justificar claves, índices, snapshots, auditoría y prevención de doble reserva.

## PREGUNTA ABIERTA

La estrategia concreta de concurrencia, retención de slots, zona horaria y representación de fechas aún no está aprobada.

## DECISIÓN — 2026-09-17 · Modelo de identidad inicial

Flyway V1 separa `users`, `roles`, `user_roles` y `refresh_sessions`. Las PK son identificadores propios, la relación usuario–rol es N:M mediante puente y cada sesión refresh referencia exactamente un usuario. Email canónico tiene UK global; documento usa UK `(document_type, document_number)`. `jti_hash` tiene UK y nunca se guarda el JWT. El índice `(user_id, revoked_at)` facilita consultas de sesiones del usuario; la búsqueda por refresh usa el UK de `jti_hash` y bloqueo de fila para rotación concurrente.

Los atributos personales dependen solo de `users.id`, nombres de rol solo de `roles.id` y vigencia/revocación solo de `refresh_sessions.id`; no hay listas ni dependencias parciales o transitivas entre atributos no clave en este corte. El modelo del resto del PRD sigue pendiente en HU-002.
