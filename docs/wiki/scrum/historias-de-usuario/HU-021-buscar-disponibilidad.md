---
id: HU-021
tipo: historia-de-usuario
titulo: "Buscar disponibilidad"
estado: Completada
epica: "[[EP-005-busqueda-y-reserva-de-citas]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-011-gestionar-afiliacion]]", "[[HU-014-gestionar-especialidades-y-duracion]]", "[[HU-016-asignar-especialidades-al-profesional]]", "[[HU-018-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-022-reservar-cita-general]]", "[[HU-023-solicitar-cita-especializada]]"]
---
# HU-021 — Buscar disponibilidad
## Historia de usuario
**COMO** USER autenticado  
**QUIERO** filtrar horarios disponibles por sede, tipo, especialidad, profesional y fecha  
**PARA** seleccionar una franja que complete la duración requerida.
## Contexto y descripción
Solo se muestran horarios que permiten todos los slots necesarios; tipo general/especializada determina el flujo posterior.
## Alcance
- Búsqueda con filtros PRD y cálculo de opciones completas para 30/60 minutos.
## Fuera de alcance
- Crear reserva, mostrar slots incompletos o especialidad/profesional inactivos.
## Reglas de negocio
- 60 min requiere dos slots consecutivos; profesional debe estar habilitado/asociado y especialidad activa.
## Dependencias y relaciones
- Épica: [[EP-005-busqueda-y-reserva-de-citas]]
- Dependencias: [[HU-011-gestionar-afiliacion]], [[HU-014-gestionar-especialidades-y-duracion]], [[HU-016-asignar-especialidades-al-profesional]], [[HU-018-crear-bloques-de-disponibilidad]].
- Relacionadas: [[HU-022-reservar-cita-general]], [[HU-023-solicitar-cita-especializada]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** consulta transversal de agenda, oferta, vigencia y slots consecutivos.
## Tareas de desarrollo
- [x] **T-01 — Diseñar filtros y contrato.** Dificultad: Medio. Cubrir sede, tipo, especialidad, profesional y fecha.
- [x] **T-02 — Calcular opciones reservables.** Dificultad: Alto. Excluir reservas/retenciones y exigir consecutividad 60 min.
- [x] **T-03 — Entregar búsqueda y pruebas.** Dificultad: Alto. Probar filtros, 30/60 y oferta inactiva/no asociada.
## Criterios de aceptación
### CA-01 — Filtros completos
**Dado** disponibilidad publicada, **cuando** USER filtra por cualquiera de los criterios permitidos, **entonces** recibe opciones que satisfacen los filtros combinados.
### CA-02 — Duración completa
**Dado** una especialidad de 60 minutos, **cuando** se consulta disponibilidad, **entonces** solo aparecen inicios con dos slots consecutivos disponibles.
### CA-03 — Oferta válida
**Dado** especialidad inactiva/no asociada o profesional no habilitado en sede, **cuando** se busca, **entonces** no aparece como opción reservable.
## Definition of Done
- [x] CA-01 a CA-03 probados en dominio/aplicación, REST y cliente aplicable.
- [x] Consultas/índices de agenda relevantes y contrato cross-repo verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `BookingIntegrationTest.hu021_ca01_searchCombinesLocationProfessionalSpecialtyAndDateFilters` | Sede, tipo (`/catalogs/specialties?type`), especialidad, profesional y fecha combinables. |
| CA-02 | Cumple | `hu021_ca02_sixtyMinuteSpecialtyOnlyOffersStartsWithTwoConsecutiveFreeSlots`; `SchedulingDomainTest.sixtyMinute…` | 60 min solo con 2 slots libres consecutivos del mismo profesional y sede. |
| CA-03 | Cumple | `hu021_ca03_inactiveOrUnassociatedOfferIsNotReturned`, `hu021_pastSlotsAreNeverOffered` | Especialidad inactiva → 404; no asociada o sede desactivada → sin opciones; nunca inicios pasados. |
| DoD cliente | Cumple | `citas-web` `BookAppointment`, `book-appointment.spec.ts` | Flujo sede → tipo → especialidad → profesional → fecha → horario. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: 23 de 24 pruebas de la Fase 3 fallaron antes de implementar (endpoints inexistentes, reserva en el pasado aceptada, PROFESSIONAL podía reservar); GREEN tras el módulo `scheduling` hexagonal. Estado `Completada`.
## Notas y decisiones
- El tratamiento de concurrencia se prueba definitivamente en las HU de reserva.
