---
id: HU-011
tipo: historia-de-usuario
titulo: "Gestionar afiliación"
estado: Pendiente de aprobación
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
- [ ] **T-01 — Definir representación de afiliación.** Dificultad: Medio. Usar FKs/relaciones normalizadas.
- [ ] **T-02 — Validar consistencia EPS-plan-régimen.** Dificultad: Alto. Impedir combinaciones inválidas o repetidas.
- [ ] **T-03 — Entregar flujo propio y pruebas.** Dificultad: Medio. Aplicar ownership y mostrar catálogos activos.
## Criterios de aceptación
### CA-01 — Asociación válida
**Dado** catálogos activos y una combinación válida, **cuando** USER guarda afiliación, **entonces** queda asociada a su perfil.
### CA-02 — Sin duplicidad
**Dado** una afiliación existente, **cuando** USER repite EPS, régimen o plan dentro de su afiliación, **entonces** la aplicación evita la duplicación definida.
### CA-03 — Aislamiento por usuario
**Dado** un USER autenticado, **cuando** consulta o modifica afiliación, **entonces** solo opera sobre su propia afiliación.
## Definition of Done
- [ ] CA-01 a CA-03 probados en dominio/REST y cliente aplicable.
- [ ] Persistencia 3FN y migración aplicable verificadas; no hay textos de catálogo duplicados.
- [ ] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
## Notas y decisiones
- La regla de vigencia de una EPS/plan se abordará con sus HU administrativas.
