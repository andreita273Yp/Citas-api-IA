---
id: HU-011
tipo: historia-de-usuario
titulo: "Gestionar afiliación"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-010-gestionar-perfil]]", "[[HU-012-gestionar-eps]]", "[[HU-013-gestionar-planes-eps]]", "[[HU-003-publicar-catalogos-fijos]]"]
relacionadas: []
---
# HU-011 — Gestionar afiliación
## Historia de usuario
**COMO** USER autenticado  
**QUIERO** asociar mi EPS, plan y régimen mediante una afiliación  
**PARA** mantener esos datos normalizados en mi cuenta.
## Contexto y descripción
EPS y planes son configurables; régimen es catálogo fijo.
## Alcance
- Consultar/crear/actualizar afiliación propia con relaciones válidas y sin duplicación interna.
## Fuera de alcance
- Validación real con aseguradoras o duplicar nombres de catálogo en USER/cita.
## Reglas de negocio
- No duplicar EPS, régimen ni plan dentro de la afiliación de un usuario; preservar ownership.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-010-gestionar-perfil]], [[HU-012-gestionar-eps]], [[HU-013-gestionar-planes-eps]], [[HU-003-publicar-catalogos-fijos]].
- Relacionadas: Ninguna.
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** enlaza catálogos configurables/fijos, integridad y ownership.
## Tareas de desarrollo
- [x] **T-01 — Definir representación de afiliación.** Dificultad: Medio. Usar FKs/relaciones normalizadas.
- [x] **T-02 — Validar consistencia EPS-plan-régimen.** Dificultad: Alto. Impedir combinaciones inválidas o repetidas.
- [x] **T-03 — Entregar flujo propio y pruebas.** Dificultad: Medio. Aplicar ownership y mostrar catálogos activos.
## Criterios de aceptación
### CA-01 — Asociación válida
**Dado** catálogos activos y una combinación válida, **cuando** USER guarda afiliación, **entonces** queda asociada a su perfil.
### CA-02 — Sin duplicidad
**Dado** una afiliación existente, **cuando** USER repite EPS, régimen o plan dentro de su afiliación, **entonces** la aplicación evita la duplicación definida.
### CA-03 — Aislamiento por usuario
**Dado** un USER autenticado, **cuando** consulta o modifica afiliación, **entonces** solo opera sobre su propia afiliación.
## Definition of Done
- [x] CA-01 a CA-03 probados en dominio/REST y cliente aplicable.
- [x] Persistencia 3FN y migración aplicable verificadas; no hay textos de catálogo duplicados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `ProfileAndAffiliationIntegrationTest.hu011_ca01_userAssociatesAValidPlanAndGetsEpsAndRegimeFromIt`, `registrationAcceptsAnOptionalActivePlan` | La afiliación referencia solo el plan; EPS y régimen se derivan de él. Sin afiliación → `204`. |
| CA-02 | Cumple | `hu011_ca02_repeatingThePlanDoesNotDuplicateAndChangingItKeepsASingleCurrentAffiliation`, `hu011_inactivePlansOrPlansOfInactiveEpsCannotBeSelected` | Repetir el plan no crea filas; cambiarlo deja una sola vigente; retirar la termina. Plan inactivo, de EPS inactiva o sin número → `400`. |
| CA-03 / DoD | Cumple | `hu011_ca03_eachUserOperatesOnlyOnOwnAffiliation` | Cada USER opera solo sobre su afiliación. `citas-web` `IdentityDashboard` ya no pide el régimen aparte (antes lo duplicaba). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Afiliación inicial opcional por FK a plan activo y actualización propia implementadas. No se almacenan nombres de EPS/plan en `users`.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- La regla de vigencia de una EPS/plan se abordará con sus HU administrativas.
