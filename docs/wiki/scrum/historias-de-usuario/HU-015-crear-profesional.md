---
id: HU-015
tipo: historia-de-usuario
titulo: "Crear profesional"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-002-modelar-persistencia-3fn]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-016-asignar-especialidades-al-profesional]]", "[[HU-017-asignar-sedes-y-estado-del-profesional]]"]
---
# HU-015 — Crear profesional
## Historia de usuario
**COMO** ADMIN  
**QUIERO** crear un usuario PROFESSIONAL con código y matrícula ficticia  
**PARA** habilitarlo posteriormente para prestar disponibilidad.
## Contexto y descripción
Los profesionales son creados por ADMIN y todos los datos del laboratorio son sintéticos.
## Alcance
- Alta de identidad PROFESSIONAL, código profesional y matrícula ficticia con validaciones aprobadas.
## Fuera de alcance
- Auto-registro PROFESSIONAL, matrícula real o asignaciones de especialidad/sede.
## Reglas de negocio
- Solo ADMIN crea PROFESSIONAL; no usar PII ni credenciales reales.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-002-modelar-persistencia-3fn]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-016-asignar-especialidades-al-profesional]], [[HU-017-asignar-sedes-y-estado-del-profesional]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** combina identidad, privilegios, unicidad y datos sintéticos.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato de alta ADMIN.** Dificultad: Medio. Documentar campos sin revelar credenciales.
- [x] **T-02 — Modelar/validar profesional.** Dificultad: Alto. Relacionarlo con usuario y preservar unicidad necesaria.
- [x] **T-03 — Integrar UI y pruebas.** Dificultad: Medio. Probar ADMIN/no ADMIN y datos sintéticos.
## Criterios de aceptación
### CA-01 — Alta autorizada
**Dado** ADMIN y datos sintéticos válidos, **cuando** crea un profesional, **entonces** se crea una identidad con rol PROFESSIONAL y sus datos profesionales.
### CA-02 — Restricción de rol
**Dado** un actor sin rol ADMIN, **cuando** intenta crear profesional, **entonces** se deniega la acción.
### CA-03 — Integridad de identidad
**Dado** datos que violan unicidad/validación, **cuando** se crea el profesional, **entonces** no se persiste un registro inconsistente.
## Definition of Done
- [x] CA-01 a CA-03 probados con autorización y persistencia.
- [x] Migración 3FN aplicable, datos sintéticos y cliente ADMIN verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `ProfessionalAdministrationIntegrationTest.hu015_ca01_adminCreatesAProfessionalIdentityThatCanSignInWithoutExposingThePassword`; `ProfessionalService.create`, `StaffAccountService` | 201 con código y matrícula; rol PROFESSIONAL; contraseña inicial solo en BCrypt y nunca en la respuesta; el profesional inicia sesión. |
| CA-02 | Cumple | `hu015_ca02_onlyAdminCanManageProfessionals`; `SecurityConfig` (`/api/v1/admin/**` → ADMIN) | USER y PROFESSIONAL → 403; sin token → 401. |
| CA-03 | Cumple | `hu015_ca03_invalidOrDuplicateDataLeavesNoInconsistentRecords` | Email, documento, código o matrícula duplicados → 409; código/matrícula vacíos o contraseña corta → 400; no quedan usuarios ni profesionales huérfanos (transacción única). |
| DoD cliente | Cumple | `AdminOffer` (pestaña Profesionales) | Alta con datos sintéticos contra REST real. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — RED 2026-10-01: las 11 pruebas de la Fase 2 fallaron con 404 (endpoints inexistentes) antes de implementar; GREEN tras implementar el módulo `offer`. Estado `Completada`.
## Notas y decisiones
- Los campos de identidad se concretan en contrato, sin contradecir RF-01/RF-07.
