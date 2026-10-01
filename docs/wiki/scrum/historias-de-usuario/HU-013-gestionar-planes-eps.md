---
id: HU-013
tipo: historia-de-usuario
titulo: "Gestionar planes de EPS"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-012-gestionar-eps]]"]
relacionadas: ["[[HU-011-gestionar-afiliacion]]"]
---
# HU-013 — Gestionar planes de EPS
## Historia de usuario
**COMO** ADMIN  
**QUIERO** gestionar planes asociados a una EPS  
**PARA** que USER seleccione combinaciones de afiliación válidas.
## Contexto y descripción
El plan depende de EPS y tampoco se borra físicamente si está referenciado.
## Alcance
- CRUD lógico de planes, asociación a EPS y filtros de planes activos aplicables.
## Fuera de alcance
- Plan sin EPS, borrado físico referenciado o reglas comerciales reales.
## Reglas de negocio
- Un plan se relaciona con EPS; las referencias mantienen integridad.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-012-gestionar-eps]].
- Relacionadas: [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** exige integridad referencial y gestión administrativa segura.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato y relación plan-EPS.** Dificultad: Medio. Validar EPS existente/activa según regla aprobada.
- [x] **T-02 — Implementar baja lógica.** Dificultad: Medio. Preservar planes referenciados.
- [x] **T-03 — Entregar UI/pruebas.** Dificultad: Medio. Cubrir relación inválida, rol y consulta.
## Criterios de aceptación
### CA-01 — Plan asociado
**Dado** una EPS válida, **cuando** ADMIN crea o actualiza un plan, **entonces** el plan queda asociado a esa EPS.
### CA-02 — Integridad de catálogo
**Dado** un plan referenciado, **cuando** ADMIN intenta eliminarlo, **entonces** se conserva y se permite desactivación cuando aplique.
### CA-03 — Selección consistente
**Dado** un consumidor de afiliación, **cuando** consulta planes de una EPS, **entonces** solo puede seleccionar planes válidos de ella conforme al contrato.
## Definition of Done
- [x] CA-01 a CA-03 probados en persistencia/REST y cliente aplicable.
- [x] Relación 3FN, migración aplicable y autorización ADMIN verificadas.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `InsuranceCatalogAdministrationIntegrationTest.hu013_ca01_planIsAssociatedToAValidEpsAndRegime` | El plan exige EPS y régimen existentes (antes se asignaba el primer régimen); código único por EPS (`409`); edición de nombre y régimen. |
| CA-02 | Cumple | `hu013_ca02_ca03_deactivatedPlanIsKeptAndNoLongerSelectable` | DELETE → `405`; desactivar conserva la fila. |
| CA-03 / DoD | Cumple | Misma prueba | `/catalogs/eps-plans` solo ofrece planes activos de EPS activas, con su régimen; afiliarse a uno inactivo → `400`. `citas-web` `AdminInsurance`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Planes asociados por FK a EPS, con alta y baja lógica REST. La creación de un registro con plan activo se cubre en prueba de integración.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- No se presupone un atributo comercial para el plan.
