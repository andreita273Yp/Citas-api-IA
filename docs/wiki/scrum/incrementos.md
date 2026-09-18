# Propuesta de incrementos y selección

Esta propuesta ordena valor y riesgo; no impone calendario, duración, horas, días ni story points. Una HU solo puede entrar al incremento cuando su estado sea `Aprobada`, sus dependencias estén `Hechas` o se seleccionen antes en la misma secuencia, y se mantengan las restricciones de [[README]]. Actualmente ninguna es seleccionable porque ninguna está aprobada.

| Incremento | Resultado verificable | Secuencia candidata de HU | Puerta de salida |
|---|---|---|---|
| S1 — Fundaciones especificadas | Modelo de datos 3FN, decisiones de concurrencia y contratos de identidad acordados. | HU-001, HU-002, HU-006 | Modelo ER, dependencias funcionales, justificación 1FN→3FN y decisiones de seguridad revisables. |
| S2 — Oferta reservable | Un USER autenticado puede encontrar una oferta válida; ADMIN puede configurar oferta y PROFESSIONAL publicar agenda. | HU-003–HU-019, respetando E01→E03 | Catálogos, profesionales, bloques y disponibilidad demuestran reglas de sede, especialidad, duración y no solape. |
| S3 — Cita controlada | Flujos de cita general y especializada, cola ADMIN y agenda profesional operan con auditoría y pruebas relevantes. | HU-020–HU-029 | No hay doble reserva; transiciones y liberación de slots tienen evidencia; contrato REST trazable. |
| S4 — Reprogramación y experiencia completa | El cliente web permite completar los flujos obligatorios y reprogramar sin pérdida de la cita original. | HU-030–HU-038 | Flujos por rol, contrato cross-repo y restricción de datos visibles verificados. |

## Reglas operativas de selección para S2, S3 y S4

1. Filtrar [[historias]] por `Estado: Aprobada` y por incremento propuesto; no inferir aprobación desde prioridad o esfuerzo.
2. Confirmar que cada dependencia listada está `Hecha`, o seleccionar primero su secuencia dentro del mismo incremento.
3. Confirmar que la decisión de datos/contrato implicada está revisada antes de iniciar una HU que modifique persistencia o REST.
4. Mantener HU no aprobadas fuera del trabajo activo y registrar las decisiones del Product Owner directamente en la HU.
5. Cerrar una HU solo si satisface sus criterios numerados, su DoD específica y el DoD común de [[README]].

## Orden de desbloqueo

`E01 → E02 → E03 → E04 → E05 → E06`.

La experiencia web puede elaborar diseño visual y contrato de consumo antes de S4, pero no debe asumir endpoints o reglas no aprobadas; el PRD fija que Stitch/AI Studio es la fuente visual de verdad del estudiante.
