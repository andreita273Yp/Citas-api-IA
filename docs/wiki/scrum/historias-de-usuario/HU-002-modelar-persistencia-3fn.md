---
id: HU-002
tipo: historia-de-usuario
titulo: "Modelar persistencia 3FN"
estado: Completada
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
- [x] **T-01 — Diseñar ER y dependencias funcionales.** Dificultad: Alto. Justificar 1FN→3FN, PK/UK y cardinalidades.
- [x] **T-02 — Modelar persistencia y restricciones.** Dificultad: Alto. Cubrir usuarios, roles, oferta, slots, citas, auditoría, tokens y reprogramación.
- [x] **T-03 — Crear migración Flyway inicial.** Dificultad: Alto. Alinear esquema, índices de agenda e integridad con el diseño.
- [x] **T-04 — Probar integridad relevante.** Dificultad: Alto. Verificar unicidad y restricciones de dominio/persistencia.
## Criterios de aceptación
### CA-01 — Normalización justificable
**Dado** el modelo, **cuando** se revisan sus relaciones, **entonces** no contiene listas, dependencias parciales ni transitivas prohibidas por el requisito 3FN.
### CA-02 — Capacidades soportadas
**Dado** el esquema, **cuando** se contrasta con el PRD, **entonces** representa las capacidades obligatorias de usuarios, agenda, citas, auditoría, tokens y reprogramación.
### CA-03 — Integridad de agenda
**Dado** el diseño de reservas, **cuando** una cita dura 60 minutos o hay reprogramación pendiente, **entonces** permite slots consecutivos y conservar la cita original hasta decisión.
## Definition of Done
- [x] CA-01 a CA-03 tienen evidencia documental y de persistencia.
- [x] La migración Flyway nueva y las pruebas de persistencia aplicables tienen resultado disponible.
- [x] Se justifican claves, cardinalidades, snapshots/FK e índices de agenda.
- [x] La trazabilidad Scrum está actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `V6__reference_3fn_model.sql`; `database/reference/README_DB.md` | Catálogos por FK, N:M con puentes (`user_roles`, `professional_specialties`, `professional_locations`), afiliación referencia el plan y desde él EPS/régimen. |
| CA-02 | Cumple | `V6__reference_3fn_model.sql` | Usuarios, roles, tokens refresh/recuperación, oferta, bloques, slots de 30 min, citas, historial y reprogramación. |
| CA-03 | Cumple | `BookingIntegrationTest` (60 min = 2 slots consecutivos, doble reserva concurrente), `AppointmentLifecycleIntegrationTest.hu027_ca01_ca02_…` (reprogramación pendiente conserva la cita original y retiene la nueva franja), `SchedulingDomainTest` | Integridad de agenda probada en Fases 3 y 5. |
| DoD unicidad | Cumple | `RegistrationIntegrationTest` (email y documento únicos, carrera resuelta por UK) | Pruebas sobre MySQL real `<DB>_test`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — Persistencia alineada al modelo de referencia con Flyway V6 (decisión registrada). CA-03 queda pendiente de prueba automatizada en la Fase 3; la HU permanece `En desarrollo`.
- 2026-10-01 — CA-03 completado con la prueba de reprogramación de la Fase 5. Estado `Completada`.
## Notas y decisiones
- La solución `database/reference/` no era fuente durante la actividad de normalización. 2026-10-01: tras esa actividad se adopta como modelo de persistencia (PRD §7; `decisions.md`).
- 2026-09-17: aprobado y en desarrollo el corte 3FN de usuarios, roles y sesiones refresh con Flyway, unicidad de email y `(tipo, número)` de documento, claves e índices justificados. El modelo restante del producto y CA/DoD globales siguen pendientes; no cerrar esta HU aún.
