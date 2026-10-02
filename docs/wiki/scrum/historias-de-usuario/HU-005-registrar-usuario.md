---
id: HU-005
tipo: historia-de-usuario
titulo: "Registrar usuario"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-001-inicializar-fundacion-tecnica]]", "[[HU-002-modelar-persistencia-3fn]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-006-iniciar-sesion]]"]
---

# HU-005 — Registrar usuario
## Historia de usuario
**COMO** visitante  
**QUIERO** crear una cuenta USER con mis datos mínimos  
**PARA** acceder al agendamiento con una identidad propia.
## Contexto y descripción
Datos mínimos: nombres, apellidos, tipo/número de documento, email, teléfono y contraseña.
## Alcance
- Contrato REST de registro, validación server-side, unicidad y persistencia segura. El formulario queda en HU-033.
## Fuera de alcance
- Registro de ADMIN/PROFESSIONAL, validación externa de identidad o datos reales.
## Reglas de negocio
- Email y documento únicos; contraseña nunca en texto plano; el visitante solo obtiene rol USER.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-001-inicializar-fundacion-tecnica]], [[HU-002-modelar-persistencia-3fn]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-006-iniciar-sesion]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** coordina identidad, seguridad, validación, datos y cliente.
## Tareas de desarrollo
- [x] **T-01 — Diseñar entrada/salida de registro.** Dificultad: Medio. Acordar campos y errores del contrato.
- [x] **T-02 — Aplicar reglas de dominio/persistencia.** Dificultad: Alto. Validar requeridos, formato y unicidad.
- [x] **T-03 — Proteger credencial.** Dificultad: Alto. Aplicar hash adaptativo y exclusión de logs/respuestas.
- [x] **T-04 — Probar contrato backend.** Dificultad: Medio. Verificar validaciones observables, duplicados y ausencia de persistencia parcial; el flujo web queda en HU-033.
## Criterios de aceptación
### CA-01 — Registro válido
**Dado** datos mínimos válidos y únicos, **cuando** el visitante se registra, **entonces** se crea una cuenta con rol USER sin exponer la contraseña.
### CA-02 — Duplicados rechazados
**Dado** email o documento existente, **cuando** se intenta registrar, **entonces** se rechaza con error del contrato sin crear otra cuenta.
### CA-03 — Datos inválidos rechazados
**Dado** campos obligatorios o formatos inválidos, **cuando** se envía el registro, **entonces** se informa validación y no se persiste una cuenta parcial.
### CA-04 — Identidad única y credencial protegida
**Dado** un email con diferencias de mayúsculas/espacios o un documento del mismo tipo y número, **cuando** se registra, **entonces** se rechaza el duplicado sin crear otra cuenta; el password solo se almacena con BCrypt y no aparece en respuesta ni logs.
## Definition of Done
- [x] CA-01 a CA-04 validados con pruebas de dominio/REST/persistencia; integración de cliente diferida a HU-033.
- [x] Hay migración/coherencia 3FN si el esquema cambia.
- [x] Hash adaptativo, validación server-side y ausencia de credenciales en logs verificados.
- [x] Trazabilidad Scrum actualizada.
- [x] CA-04 y restricciones únicas Flyway probadas en REST/persistencia; `mvn test` pasa en Java 21/MySQL 8.4.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `RegistrationIntegrationTest.ca01_createsUserAccountWithOnlyUserRoleAndWithoutExposingPassword`; `RegistrationService` | 201, solo rol USER (un `roles` enviado se ignora) y respuesta sin password ni hash. |
| CA-02 | Cumple | `RegistrationIntegrationTest.ca02_ca04_rejectsSameEmail…`, `ca02_ca04_rejectsSameDocument…`; UK `uq_users_email`, `uq_users_document` (V6) | 409 sin segunda cuenta; una carrera la resuelve la UK y se traduce a 409. |
| CA-03 | Cumple | `RegistrationIntegrationTest.ca03_rejectsMissingOrInvalidDataWithoutPersistingAPartialAccount` (5 casos); `PersonalData`, `EmailAddress`, `PasswordPolicy` | Campo ausente, email inválido, contraseña corta, teléfono vacío y contraseña > 72 bytes → 400 sin filas. |
| CA-04 | Cumple | `EmailAddress.of`, `Document`; `RegistrationIntegrationTest.ca04_storesPasswordOnlyAsBcryptHash` | Email con espacios/mayúsculas se normaliza antes de validar; solo se guarda hash BCrypt. |
| DoD pruebas | Cumple | `RegistrationIntegrationTest` (9), `auth-api.spec.ts` | MySQL real; cliente Angular registra contra la URL configurable. |
| DoD esquema/seguridad | Cumple | `V6__reference_3fn_model.sql`, `data-integrity.md`, `AuthController`, `RegistrationService` | Validación server-side en dominio; sin password en respuesta ni logs. |
| DoD trazabilidad | Cumple | Esta HU, `contracts.md`, `traceability.md` | Formulario real de registro implementado y verificado visualmente en localhost. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-17 — Corte backend aprobado, validado y completado con `mvn test` (8/8); tareas de UI movidas a HU-033.
- 2026-09-24 — Fase 1 conectó el formulario Angular a `POST /auth/register`; lint, Vitest (5/5) y build pasaron en Docker. Estado `Completada`.
- 2026-10-01 — Revalidación: las pruebas citadas antes ya no existían en el repositorio. Se reescribieron; RED real en `ca02_ca04_rejectsSameEmailIgnoringCaseAndSpaces…` (400 en lugar de 409: `@Email` validaba antes de normalizar). Corregido moviendo la validación al dominio; GREEN. Se mantiene `Completada`.
## Notas y decisiones
- El diseño visual requiere aprobación fuera de esta especificación.
- 2026-09-17: usuario aprobó el corte backend. Formulario y flujo visual pasan a HU-033; su ausencia no bloquea la DoD backend de HU-005. Documento único por tipo+número, email normalizado sin distinguir mayúsculas. No se implementa registro de otros roles.
