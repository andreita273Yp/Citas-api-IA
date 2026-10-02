---
id: HU-026
tipo: historia-de-usuario
titulo: "Cancelar cita"
estado: Completada
epica: "[[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 5"
dependencias: ["[[HU-025-consultar-mis-citas]]"]
relacionadas: ["[[HU-032-consultar-auditoria-de-estados]]"]
---
# HU-026 — Cancelar cita
## Historia de usuario
**COMO** USER  
**QUIERO** cancelar una cita futura no terminal  
**PARA** liberar su franja cuando ya no la necesito.
## Contexto y descripción
Una cancelada no se reactiva directamente y debe registrarse historial.
## Alcance
- Validar ownership/estado/futuro, transición `CANCELLED`, liberación y auditoría.
## Fuera de alcance
- Reactivación directa o cancelación de cita terminal/no futura.
## Reglas de negocio
- `CANCELLED` libera slots; transiciones explícitas e historial obligatorio.
## Dependencias y relaciones
- Épica: [[EP-006-ciclo-de-vida-de-citas-y-reprogramaciones]]
- Dependencias: [[HU-025-consultar-mis-citas]].
- Relacionadas: [[HU-032-consultar-auditoria-de-estados]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** cambia estado y disponibilidad sin permitir transiciones inválidas.
## Tareas de desarrollo
- [x] **T-01 — Definir elegibilidad.** Dificultad: Alto. Validar futuro, no terminal y ownership.
- [x] **T-02 — Aplicar cancelación atómica.** Dificultad: Alto. Actualizar estado, liberar slots y auditar.
- [x] **T-03 — Integrar acción/pruebas.** Dificultad: Medio. Cubrir éxito, inválida y visibilidad posterior.
## Criterios de aceptación
### CA-01 — Cancelación permitida
**Dado** una cita propia futura no terminal, **cuando** USER cancela, **entonces** cambia a `CANCELLED` y libera sus slots.
### CA-02 — Cancelación restringida
**Dado** cita ajena, pasada o terminal, **cuando** USER intenta cancelar, **entonces** se rechaza sin modificarla.
### CA-03 — No reactivación directa
**Dado** una cita `CANCELLED`, **cuando** se intenta restaurarla por la misma capacidad, **entonces** no se permite y el historial conserva la cancelación.
## Definition of Done
- [x] CA-01 a CA-03 probados con estado, slots, ownership y auditoría.
- [x] Contrato/cliente y persistencia/índices aplicables verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `AppointmentLifecycleIntegrationTest`.hu026_ca01_cancellingAFutureOwnAppointmentReleasesItsSlotsAndIsAudited, cancellingWithAPendingRescheduleReleasesBothRangesAndCancelsTheRequest | CANCELLED, slots liberados y reservables; con reprogramación pendiente se liberan ambas franjas y la solicitud queda CANCELLED; historial fuente USER. |
| CA-02 | Cumple | `AppointmentLifecycleIntegrationTest`.hu026_ca02_ca03_foreignPastOrTerminalAppointmentsCannotBeCancelledOrReactivated | Ajena → 404; pasada o terminal → 409 sin cambios. |
| CA-03 / DoD | Cumple | Misma prueba; `AppointmentStatus.transitionTo` | No existe transición desde `CANCELLED`; recancelar o reprogramar → 409; el historial conserva una sola cancelación. `citas-web` `MyAppointments` (Cancelar con confirmación). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: 8 de 11 pruebas fallaron antes de implementar (respuestas sin sede/profesional, reprogramación sin 201 ni estado visible, sin decisión del paciente tras rechazo); GREEN tras mover el ciclo de vida a `scheduling` hexagonal. Estado `Completada`.
## Notas y decisiones
- El catálogo determina cuáles estados son terminales.
