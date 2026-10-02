---
id: HU-030
tipo: historia-de-usuario
titulo: "Cerrar atención"
estado: Completada
epica: "[[EP-007-operacion-profesional-y-administrativa]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 6"
dependencias: ["[[HU-029-consultar-agenda-profesional]]"]
relacionadas: ["[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-030 — Cerrar atención
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** marcar una cita pasada/aplicable como `COMPLETED` o `NO_SHOW`  
**PARA** cerrar su atención con trazabilidad.
## Contexto y descripción
El PRD no permite que PROFESSIONAL haga otras decisiones de cita.
## Alcance
- Validar cita propia y aplicable, transición explícita e historial.
## Fuera de alcance
- Cerrar cita ajena/no aplicable, cancelar o aprobar solicitudes.
## Reglas de negocio
- Solo `COMPLETED`/`NO_SHOW`; debe registrarse historial.
## Dependencias y relaciones
- Épica: [[EP-007-operacion-profesional-y-administrativa]]
- Dependencias: [[HU-029-consultar-agenda-profesional]].
- Relacionadas: [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** requiere transición de estado, temporalidad y ownership rigurosos.
## Tareas de desarrollo
- [x] **T-01 — Definir aplicabilidad.** Dificultad: Alto. Traducir “pasada/aplicable” a condición verificable aprobada.
- [x] **T-02 — Aplicar transición/auditoría.** Dificultad: Alto. Restringir actor/estado y guardar fuente/fecha.
- [x] **T-03 — Integrar acción/pruebas.** Dificultad: Medio. Cubrir ambos resultados y casos denegados.
## Criterios de aceptación
### CA-01 — Cierre permitido
**Dado** una cita propia pasada/aplicable, **cuando** PROFESSIONAL selecciona `COMPLETED` o `NO_SHOW`, **entonces** se registra el estado elegido.
### CA-02 — Restricción de propiedad/aplicabilidad
**Dado** una cita ajena o no aplicable, **cuando** PROFESSIONAL intenta cerrarla, **entonces** la aplicación lo impide.
### CA-03 — Auditoría
**Dado** un cierre exitoso, **cuando** se consulta historial, **entonces** consta cita, estado nuevo, actor/fuente y fecha/hora aplicables.
## Definition of Done
- [x] CA-01 a CA-03 probados en dominio/REST, autorización y auditoría.
- [x] La definición operativa de “aplicable” queda acordada/documentada antes de completar.
- [x] Cliente y trazabilidad Scrum actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `OperationsIntegrationTest`.hu030_ca01_ca03_professionalClosesPastOwnAppointmentsAndTheClosureIsAudited | Cita propia `APPROVED` terminada → `COMPLETED` o `NO_SHOW`. |
| CA-02 | Cumple | `OperationsIntegrationTest`.hu030_ca02_foreignFutureOrNonApplicableAppointmentsCannotBeClosed | Futura, `REQUESTED` o ya cerrada → 409; ajena → 404; estado distinto de COMPLETED/NO_SHOW → 400; USER → 403. |
| CA-03 / DoD | Cumple | Misma prueba CA-01 | Historial: `APPROVED → COMPLETED`, actor = usuario del profesional, fuente `USER` (el modelo solo admite SYSTEM/USER/ADMIN), fecha. `citas-web` `OperationsPanel`: botones de cierre solo si `closable`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: 5 de 8 pruebas fallaron antes de implementar (agenda sin sede ni duración, sin vista día/semana, cierre sin estado en la respuesta, bandeja con ids como texto, historial sin garantía de solo-inserción); GREEN tras mover operaciones a `scheduling` hexagonal (`OperationsService`). Estado `Completada`.
## Notas y decisiones
- Pregunta abierta: definición de condición “aplicable”.
