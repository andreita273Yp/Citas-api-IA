---
id: HU-020
tipo: historia-de-usuario
titulo: "Consultar calendario de disponibilidad"
estado: Completada
epica: "[[EP-004-disponibilidad-del-profesional]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 4"
dependencias: ["[[HU-018-crear-bloques-de-disponibilidad]]"]
relacionadas: ["[[HU-029-consultar-agenda-profesional]]"]
---
# HU-020 — Consultar calendario de disponibilidad
## Historia de usuario
**COMO** PROFESSIONAL  
**QUIERO** consultar mi calendario de bloques  
**PARA** conocer la disponibilidad que publiqué.
## Contexto y descripción
La agenda de disponibilidad no sustituye la agenda visible de citas aprobadas.
## Alcance
- Consulta del calendario/bloques propios con sede y fecha.
## Fuera de alcance
- Datos de citas de otros profesionales o modificación desde esta consulta.
## Reglas de negocio
- Solo ownership; mostrar bloques/sedes sin exponer información de USER.
## Dependencias y relaciones
- Épica: [[EP-004-disponibilidad-del-profesional]]
- Dependencias: [[HU-018-crear-bloques-de-disponibilidad]].
- Relacionadas: [[HU-029-consultar-agenda-profesional]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** requiere filtros y aislamiento de datos en una vista de calendario.
## Tareas de desarrollo
- [x] **T-01 — Definir consulta/filtros.** Dificultad: Medio. Acordar fecha/sede y representación sin imponer UI.
- [x] **T-02 — Aplicar ownership.** Dificultad: Medio. Restringir al calendario del autenticado.
- [x] **T-03 — Entregar calendario y pruebas.** Dificultad: Medio. Probar filtro y ausencia de agenda ajena.
## Criterios de aceptación
### CA-01 — Visualización propia
**Dado** bloques propios publicados, **cuando** PROFESSIONAL consulta su calendario, **entonces** ve fecha, franja y sede de sus bloques.
### CA-02 — Filtro aplicable
**Dado** bloques en diversas fechas/sedes, **cuando** aplica los filtros del contrato, **entonces** recibe solo los bloques coincidentes.
### CA-03 — Aislamiento
**Dado** otro profesional, **cuando** intenta consultar calendario ajeno, **entonces** no obtiene esos bloques.
## Definition of Done
- [x] CA-01 a CA-03 probados en autorización/REST y cliente aplicable.
- [x] No se exponen datos de USER ni información ajena; trazabilidad actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `AgendaBlocksIntegrationTest.hu020_professionalSeesOnlyOwnBlocksFilteredByDateAndLocation` | Fecha, franja, sede, slots y ocupados; sin datos de pacientes. |
| CA-02 | Cumple | Misma prueba | Filtros `from`, `to`, `locationId`. |
| CA-03 | Cumple | Misma prueba; `onlyProfessionalsManageBlocks` | Cada profesional ve solo sus bloques; USER/ADMIN → 403. |
| DoD cliente | Cumple | `citas-web` `ProfessionalAgenda` (Calendario con filtros) | Contra REST real. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: 23 de 24 pruebas de la Fase 3 fallaron antes de implementar (endpoints inexistentes, reserva en el pasado aceptada, PROFESSIONAL podía reservar); GREEN tras el módulo `scheduling` hexagonal. Estado `Completada`.
## Notas y decisiones
- El formato visual queda bajo el diseño aprobado.
