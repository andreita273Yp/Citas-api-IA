---
id: HU-002
tipo: historia-de-usuario
titulo: "Modelar persistencia 3FN"
estado: En desarrollo
epica: "[[EP-001-fundacion-y-contrato-del-producto]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 1"
dependencias: ["[[HU-001-inicializar-fundacion-tecnica]]"]
relacionadas: ["[[HU-003-publicar-catalogos-fijos]]"]
---

# HU-002 — Modelar persistencia 3FN
## Historia de usuario
**COMO** equipo de desarrollo  
**QUIERO** contar con un modelo persistente normalizado para el dominio  
**PARA** preservar integridad y evitar duplicidades en las capacidades de citas.
## Contexto y descripción
Debe soportar entidades/capacidades del requisito 3FN, sin copiar la solución de referencia del trainer ni imponer tablas antes del diseño.
## Alcance
- Modelo, dependencias funcionales, cardinalidades, claves/índices y migración inicial coherente.
## Fuera de alcance
- Datos de producción, migraciones ejecutadas o cambios de reglas aún no aprobadas.
## Reglas de negocio
- 1FN/2FN/3FN; N:M con puentes; catálogos por FK; reserva y reprogramación íntegra.
## Dependencias y relaciones
- Épica: [[EP-001-fundacion-y-contrato-del-producto]]
- Dependencias: [[HU-001-inicializar-fundacion-tecnica]].
- Relacionadas: [[HU-003-publicar-catalogos-fijos]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** coordina todo el dominio, integridad y acceso a agenda.
## Tareas de desarrollo
- [ ] **T-01 — Diseñar ER y dependencias funcionales.** Dificultad: Alto. Justificar 1FN→3FN, PK/UK y cardinalidades.
- [ ] **T-02 — Modelar persistencia y restricciones.** Dificultad: Alto. Cubrir usuarios, roles, oferta, slots, citas, auditoría, tokens y reprogramación.
- [ ] **T-03 — Crear migración Flyway inicial.** Dificultad: Alto. Alinear esquema, índices de agenda e integridad con el diseño.
- [ ] **T-04 — Probar integridad relevante.** Dificultad: Alto. Verificar unicidad y restricciones de dominio/persistencia.
## Criterios de aceptación
### CA-01 — Normalización justificable
**Dado** el modelo, **cuando** se revisan sus relaciones, **entonces** no contiene listas, dependencias parciales ni transitivas prohibidas por el requisito 3FN.
### CA-02 — Capacidades soportadas
**Dado** el esquema, **cuando** se contrasta con el PRD, **entonces** representa las capacidades obligatorias de usuarios, agenda, citas, auditoría, tokens y reprogramación.
### CA-03 — Integridad de agenda
**Dado** el diseño de reservas, **cuando** una cita dura 60 minutos o hay reprogramación pendiente, **entonces** permite slots consecutivos y conservar la cita original hasta decisión.
## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia documental y de persistencia.
- [ ] La migración Flyway nueva y las pruebas de persistencia aplicables tienen resultado disponible.
- [ ] Se justifican claves, cardinalidades, snapshots/FK e índices de agenda.
- [ ] La trazabilidad Scrum está actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | — |
| CA-02 | Pendiente | — | — |
| CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
## Notas y decisiones
- La solución `database/reference/` no es fuente de este mapa.
- 2026-09-17: aprobado y en desarrollo el corte 3FN de usuarios, roles y sesiones refresh con Flyway, unicidad de email y `(tipo, número)` de documento, claves e índices justificados. El modelo restante del producto y CA/DoD globales siguen pendientes; no cerrar esta HU aún.
