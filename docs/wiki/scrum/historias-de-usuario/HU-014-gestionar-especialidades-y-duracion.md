---
id: HU-014
tipo: historia-de-usuario
titulo: "Gestionar especialidades y duración"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-002-modelar-persistencia-3fn]]"]
relacionadas: ["[[HU-016-asignar-especialidades-al-profesional]]", "[[HU-021-buscar-disponibilidad]]"]
---
# HU-014 — Gestionar especialidades y duración
## Historia de usuario
**COMO** ADMIN  
**QUIERO** gestionar especialidades y su duración de 30 o 60 minutos  
**PARA** controlar la duración requerida en la reserva.
## Contexto y descripción
El profesional no puede sobrescribir la duración de una especialidad.
## Alcance
- CRUD lógico de especialidades activas y duración restringida a 30/60.
## Fuera de alcance
- Duraciones distintas, ajuste por profesional o borrado físico referenciado.
## Reglas de negocio
- 30 min equivale a un slot; 60 a dos consecutivos; especialidad inactiva no es reservable.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-002-modelar-persistencia-3fn]].
- Relacionadas: [[HU-016-asignar-especialidades-al-profesional]], [[HU-021-buscar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** su configuración determina el algoritmo de disponibilidad y reserva.
## Tareas de desarrollo
- [x] **T-01 — Definir catálogo/contrato.** Dificultad: Medio. Limitar duración a valores autorizados.
- [x] **T-02 — Aplicar validación y baja lógica.** Dificultad: Alto. Evitar duración inválida o eliminación referenciada.
- [x] **T-03 — Conectar a consulta/reserva.** Dificultad: Alto. Exponer duración como fuente única de slots.
- [x] **T-04 — Probar reglas.** Dificultad: Alto. Cubrir 30, 60, activa/inactiva y rol ADMIN.
## Criterios de aceptación
### CA-01 — Duración restringida
**Dado** un ADMIN, **cuando** configura una especialidad, **entonces** solo puede establecer 30 o 60 minutos.
### CA-02 — Fuente única
**Dado** una especialidad asignada, **cuando** se consulta para reservar, **entonces** su duración se toma del catálogo y no del profesional.
### CA-03 — Vigencia protegida
**Dado** una especialidad referenciada, **cuando** ADMIN intenta retirarla, **entonces** no se borra físicamente y la inactividad impide nuevas reservas.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/REST y cliente ADMIN aplicable.
- [x] Persistencia 3FN/migración aplicable y efecto sobre slots verificables.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `SpecialtyAdministrationIntegrationTest.ca01_adminCreatesSpecializedSpecialtiesOnlyWith30Or60Minutes`, `ca01_rejectsDuplicatesAndNonAdminActors`; `AppointmentDuration` | 0, 15, 45 y 90 min → 400 al crear y al editar; código/nombre duplicado → 409; USER → 403. |
| CA-02 | Cumple | `SpecialtyAdministrationIntegrationTest.ca02_availabilityTakesTheDurationFromTheSpecialtyCatalog`; `SchedulingController.duration` | Con un bloque de 2 h: 3 horarios de 60 min; tras cambiar a 30 min, 4 horarios. El profesional no define duración. |
| CA-03 | Cumple | `SpecialtyAdministrationIntegrationTest.ca03_retiringASpecialtyDeactivatesItWithoutDeletingAndBlocksNewBookings` | DELETE → 405; desactivar conserva la fila, la quita del catálogo y de la búsqueda, y la reserva responde 404 sin crear cita. |
| DoD cliente | Cumple | `citas-web` `AdminOffer` (pestaña Especialidades), `offer-api.spec.ts` | Crear, cambiar duración y activar/desactivar contra REST real. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: las 11 pruebas de la Fase 2 fallaron con 404 (endpoints inexistentes) antes de implementar; GREEN tras implementar el módulo `offer`. Estado `Completada`.
## Notas y decisiones
- Medicina General debe estar representada por el catálogo aprobado.
