---
id: HU-009
tipo: historia-de-usuario
titulo: "Restablecer contraseña"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-008-solicitar-recuperacion-de-contrasena]]"]
relacionadas: ["[[HU-007-renovar-y-cerrar-sesion]]"]
---
# HU-009 — Restablecer contraseña
## Historia de usuario
**COMO** usuario con token de recuperación válido  
**QUIERO** definir una nueva contraseña  
**PARA** recuperar mi acceso sin reutilizar el token.
## Contexto y descripción
Cambiar contraseña consume/invalida el token de recuperación.
## Alcance
- Validar token, cambiar hash, consumir token y actualizar sesión según política aprobada.
## Fuera de alcance
- Cambio de contraseña dentro de perfil o envío SMTP obligatorio.
## Reglas de negocio
- Token temporal de único uso; contraseña nunca se persiste en texto plano.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-008-solicitar-recuperacion-de-contrasena]].
- Relacionadas: [[HU-007-renovar-y-cerrar-sesion]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** altera credenciales y debe impedir reutilización y sesiones indebidas.
## Tareas de desarrollo
- [x] **T-01 — Validar/consumir token.** Dificultad: Alto. Aplicar vigencia y uso único de forma atómica.
- [x] **T-02 — Actualizar contraseña segura.** Dificultad: Alto. Aplicar hash adaptativo y política de sesión aprobada.
- [x] **T-03 — Integrar formulario y pruebas.** Dificultad: Medio. Validar nueva contraseña, token inválido y reutilización.
## Criterios de aceptación
### CA-01 — Restablecimiento válido
**Dado** un token vigente no usado y una contraseña válida, **cuando** se confirma el cambio, **entonces** la nueva contraseña permite autenticación posterior.
### CA-02 — Consumo del token
**Dado** un token usado, vencido o inválido, **cuando** se intenta restablecer, **entonces** se rechaza y no cambia la contraseña.
### CA-03 — Seguridad posterior
**Dado** una contraseña cambiada, **cuando** se revisa la persistencia y sesiones afectadas, **entonces** no hay texto plano y se aplica la invalidez de sesión definida en el contrato.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas de dominio/seguridad y REST aplicable.
- [x] Token consumido y hash adaptativo verificados; migración si corresponde.
- [x] Cliente y trazabilidad Scrum actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `PasswordRecoveryIntegrationTest.hu009_ca01_validTokenChangesThePasswordForLaterSignIn` | `204`; la nueva contraseña inicia sesión y la anterior ya no. |
| CA-02 | Cumple | `hu009_ca02_usedExpiredOrInvalidTokensAreRejectedWithoutChangingThePassword` | Inválido, vencido o ya usado → `401` sin cambiar la contraseña; confirmación distinta o contraseña débil → `400`. |
| CA-03 / DoD | Cumple | `hu009_ca03_passwordIsStoredAsBcryptAndOpenSessionsAreRevoked` | Hash BCrypt; token marcado `used_at`; todas las sesiones refresh del usuario se revocan (política documentada con HU-007). `citas-web` `Login` (Recuperar acceso / Ya tengo un token). |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Implementado consumo atómico de token, BCrypt y revocación de refresh sessions. `IdentityExtensionIntegrationTest` verifica restablecimiento, reutilización rechazada y nuevo login.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- La política de sesiones posteriores debe documentarse con [[HU-007-renovar-y-cerrar-sesion]].
