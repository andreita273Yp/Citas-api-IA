---
id: HU-024
tipo: historia-de-usuario
titulo: "Resolver solicitud especializada"
estado: Completada
epica: "[[EP-005-busqueda-y-reserva-de-citas]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-023-solicitar-cita-especializada]]"]
relacionadas: ["[[HU-031-consultar-bandeja-administrativa]]", "[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-024 — Resolver solicitud especializada
## Historia de usuario
**COMO** ADMIN  
**QUIERO** aprobar o rechazar una cita especializada solicitada  
**PARA** decidir la atención y liberar la franja cuando corresponda.
## Contexto y descripción
El rechazo exige motivo; aprobar cambia a `APPROVED`, rechazar a `REJECTED` y libera slots.
## Alcance
- Decisión ADMIN sobre `REQUESTED`, validación de transición, motivo de rechazo, slots e historial.
## Fuera de alcance
- Decidir citas generales o modificar selección clínica.
## Reglas de negocio
- Rechazo exige motivo; transiciones explícitas; rechazo libera la reserva.
## Dependencias y relaciones
- Épica: [[EP-005-busqueda-y-reserva-de-citas]]
- Dependencias: [[HU-023-solicitar-cita-especializada]].
- Relacionadas: [[HU-031-consultar-bandeja-administrativa]], [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** coordina autorización, transición, liberación y auditoría.
## Tareas de desarrollo
- [x] **T-01 — Definir decisión ADMIN.** Dificultad: Medio. Acordar precondición `REQUESTED`, motivo y errores.
- [x] **T-02 — Aplicar transición atómica.** Dificultad: Alto. Aprobar o rechazar/liberar sin estados intermedios.
- [x] **T-03 — Integrar bandeja/pruebas.** Dificultad: Alto. Cubrir rol, motivo obligatorio, slots e historial.
## Criterios de aceptación
### CA-01 — Aprobación
**Dado** una cita `REQUESTED`, **cuando** ADMIN aprueba, **entonces** pasa a `APPROVED` y conserva la reserva.
### CA-02 — Rechazo con motivo
**Dado** una cita `REQUESTED`, **cuando** ADMIN rechaza con motivo, **entonces** pasa a `REJECTED`, registra motivo y libera slots.
### CA-03 — Decisión válida
**Dado** actor no ADMIN, estado distinto de `REQUESTED` o rechazo sin motivo, **cuando** intenta decidir, **entonces** se rechaza sin transición.
## Definition of Done
- [x] CA-01 a CA-03 probados con persistencia/REST, cliente ADMIN y auditoría.
- [x] Transición/liberación atómica y contrato cross-repo verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `SpecializedDecisionIntegrationTest.ca01_approvingKeepsTheReservationAndIsAuditedAsAdmin` | `APPROVED`, slots conservados, `approved_by/at`, historial REQUESTED→APPROVED fuente ADMIN. |
| CA-02 | Cumple | `ca02_rejectingWithReasonRecordsItAndReleasesTheSlots` | `REJECTED`, motivo en historial y en `decisionReason`, slots liberados y reservables otra vez. |
| CA-03 | Cumple | `ca03_rejectionWithoutReasonIsRefusedWithoutTransition`, `ca03_onlyRequestedAppointmentsCanBeDecided`, `ca03_nonAdminActorsCannotDecide` | Sin motivo o decisión inválida → 400; ya decidida → 409; inexistente → 404; USER/PROFESSIONAL → 403; sin transición. |
| DoD cliente | Cumple | `citas-web` `OperationsPanel` (bandeja ADMIN) | Aprobar y Rechazar con motivo obligatorio. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: 23 de 24 pruebas de la Fase 3 fallaron antes de implementar (endpoints inexistentes, reserva en el pasado aceptada, PROFESSIONAL podía reservar); GREEN tras el módulo `scheduling` hexagonal. Estado `Completada`.
## Notas y decisiones
- La bandeja se especifica en [[HU-031-consultar-bandeja-administrativa]].
