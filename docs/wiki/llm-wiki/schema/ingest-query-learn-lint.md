# Workflow INGEST / QUERY / LEARN / LINT

## INGEST

1. Verificar que la fuente es aprobada y segura para persistir.
2. Conservar una copia inmutable en `raw/` o registrar por qué no se copia.
3. Integrar solo conocimiento durable en páginas existentes.
4. Añadir enlaces cruzados, actualizar `wiki/index.md` y anexar una entrada a `wiki/log.md`.

## QUERY

1. Leer `wiki/index.md`.
2. Leer las páginas pertinentes y sus fuentes enlazadas.
3. Contrastar contra código y pruebas si existen.
4. Responder separando evidencia e inferencia.

## LEARN

Después de una interacción sustancial, persistir solo conocimiento durable. Clasificarlo como HECHO, DECISIÓN, PREFERENCIA o PREGUNTA ABIERTA. Verificar los hechos antes de escribirlos.

## LINT

Comprobar contradicciones, claims obsoletos, duplicados, páginas huérfanas, enlaces rotos, decisiones no aprobadas y contenido sensible. Registrar hallazgos y correcciones en `wiki/log.md`.
