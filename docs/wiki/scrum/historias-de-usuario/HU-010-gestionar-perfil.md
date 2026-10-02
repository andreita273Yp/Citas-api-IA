---
id: HU-010
tipo: historia-de-usuario
titulo: "Gestionar perfil"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-006-iniciar-sesion]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-011-gestionar-afiliacion]]"]
---
# HU-010 — Gestionar perfil
## Historia de usuario
**COMO** USER autenticado  
**QUIERO** consultar y actualizar los datos permitidos de mi perfil  
**PARA** mantener mi información de contacto vigente.
## Contexto y descripción
El PRD no enumera cuáles campos de identidad pueden cambiar; esa limitación debe resolverse en contrato sin inventarla.
## Alcance
- Consulta por ownership, edición de campos permitidos aprobados y validación server-side.
## Fuera de alcance
- Cambio de rol, acceso a perfil ajeno, y campos no autorizados por contrato.
## Reglas de negocio
- Ownership obligatorio; unicidad se preserva si los campos editables incluyen email/documento.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-006-iniciar-sesion]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-011-gestionar-afiliacion]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** combina ownership, validación y ambigüedad de campos permitidos.
## Tareas de desarrollo
- [x] **T-01 — Acordar campos editables.** Dificultad: Medio. Documentar el límite sin ampliar el PRD.
- [x] **T-02 — Implementar consulta/actualización propia.** Dificultad: Medio. Aplicar ownership, validación y unicidad.
- [x] **T-03 — Integrar pantalla y pruebas.** Dificultad: Medio. Mostrar datos y errores de validación autorizados.
## Criterios de aceptación
### CA-01 — Consulta propia
**Dado** un USER autenticado, **cuando** consulta su perfil, **entonces** recibe únicamente sus datos permitidos.
### CA-02 — Actualización válida
**Dado** cambios permitidos y válidos, **cuando** los guarda, **entonces** quedan disponibles al volver a consultar el perfil.
### CA-03 — Protección de datos
**Dado** una solicitud para otro usuario o datos inválidos/duplicados, **cuando** se procesa, **entonces** se deniega o valida sin modificar información no permitida.
## Definition of Done
- [x] CA-01 a CA-03 validados con pruebas de ownership, validación y cliente aplicable.
- [x] El contrato enumera los campos permitidos antes de completar la HU.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `ProfileAndAffiliationIntegrationTest.hu010_ca01_userReadsOnlyOwnAllowedData` | Datos propios sin password ni hash; sin sesión 401. El perfil se resuelve por el token, no por id en la ruta. |
| CA-02 | Cumple | `hu010_ca02_ca03_onlyThePhoneCanBeUpdatedAndItIsValidated` | El teléfono actualizado persiste al volver a consultar. |
| CA-03 / DoD | Cumple | Misma prueba; `PersonalData.phone` | Email, nombre o roles enviados se ignoran; teléfono vacío, sin dígitos o con menos de 7 o más de 15 dígitos → `400`. `citas-web` `IdentityDashboard`. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — El contrato limita `PATCH /users/me` a teléfono. La prueba de integración cubre lectura propia y cambio de teléfono; pantalla Angular conectada a REST.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- Pregunta abierta: campos exactos editables.
