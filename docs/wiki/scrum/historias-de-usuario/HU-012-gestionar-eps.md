---
id: HU-012
tipo: historia-de-usuario
titulo: "Gestionar EPS"
estado: Completada
epica: "[[EP-003-administracion-de-catalogos-y-profesionales]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 3"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-002-modelar-persistencia-3fn]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-013-gestionar-planes-eps]]", "[[HU-011-gestionar-afiliacion]]"]
---
# HU-012 — Gestionar EPS
## Historia de usuario
**COMO** ADMIN  
**QUIERO** crear, consultar, actualizar y activar/desactivar EPS  
**PARA** mantener el catálogo disponible para afiliaciones.
## Contexto y descripción
Es catálogo configurable; no se borra físicamente si está referenciado.
## Alcance
- CRUD lógico de EPS con autorización ADMIN y consulta de catálogo aplicable.
## Fuera de alcance
- Borrado físico de EPS referenciada o integración con EPS real.
## Reglas de negocio
- Referencias transaccionales se preservan mediante activación/desactivación.
## Dependencias y relaciones
- Épica: [[EP-003-administracion-de-catalogos-y-profesionales]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-002-modelar-persistencia-3fn]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-013-gestionar-planes-eps]], [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina CRUD, referencias, seguridad y contrato.
## Tareas de desarrollo
- [x] **T-01 — Modelar operaciones ADMIN.** Dificultad: Medio. Definir validaciones y contrato.
- [x] **T-02 — Aplicar baja lógica.** Dificultad: Medio. Impedir borrado físico cuando hay referencias.
- [x] **T-03 — Integrar UI y pruebas.** Dificultad: Medio. Probar ADMIN/no ADMIN y catálogo activo.
## Criterios de aceptación
### CA-01 — CRUD autorizado
**Dado** un ADMIN, **cuando** gestiona una EPS válida, **entonces** puede crearla, consultarla o actualizarla según contrato.
### CA-02 — Protección de referencias
**Dado** una EPS referenciada, **cuando** ADMIN intenta retirarla, **entonces** no se borra físicamente y puede desactivarse.
### CA-03 — Restricción de rol
**Dado** un actor distinto de ADMIN, **cuando** intenta gestionar EPS, **entonces** se deniega.
## Definition of Done
- [x] CA-01 a CA-03 probados por rol y persistencia.
- [x] Migración/índices aplicables y cliente ADMIN verificados.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `InsuranceCatalogAdministrationIntegrationTest.hu012_ca01_adminCreatesReadsAndUpdatesEps` | Crear (`201`), listar y renombrar; código o nombre duplicado → `409`; datos vacíos → `400`; inexistente → `404`. |
| CA-02 | Cumple | `hu012_ca02_referencedEpsIsDeactivatedNotDeletedAndLeavesThePublicCatalog` | DELETE → `405`; desactivar conserva la fila y la afiliación existente, y saca la EPS y sus planes del catálogo de selección. |
| CA-03 / DoD | Cumple | `hu012_ca03_hu013_onlyAdminManagesInsuranceCatalogs`; `SecurityConfig` | USER → `403`, sin sesión → `401`. `citas-web` `AdminInsurance` (pestaña EPS y planes). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — CRUD lógico REST y pantalla ADMIN implementados; prueba de integración confirma rechazo a USER y alta por ADMIN.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- Los campos concretos del catálogo deben aprobarse en el contrato.
