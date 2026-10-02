---
id: HU-006
tipo: historia-de-usuario
titulo: "Iniciar sesión"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-005-registrar-usuario]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-007-renovar-y-cerrar-sesion]]"]
---

# HU-006 — Iniciar sesión
## Historia de usuario
**COMO** usuario registrado  
**QUIERO** iniciar sesión con email y contraseña  
**PARA** acceder a las capacidades correspondientes a mi rol.
## Contexto y descripción
La sesión emite access token de corta duración y refresh token separado; roles participan en autorización.
## Alcance
- Autenticación y emisión segura de tokens por REST; el manejo visual queda en HU-033.
## Fuera de alcance
- SSO, MFA o recuperación de contraseña.
## Reglas de negocio
- Credenciales inválidas no revelan información sensible; tokens no se registran en log.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-005-registrar-usuario]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-007-renovar-y-cerrar-sesion]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** combina autenticación, tokens, roles y seguridad de cliente.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato de autenticación.** Dificultad: Alto. Acordar manejo de access/refresh y errores.
- [x] **T-02 — Validar credenciales y roles.** Dificultad: Alto. Comparar hash y construir contexto de autorización.
- [x] **T-03 — Documentar contrato para cliente futuro.** Dificultad: Medio. Definir éxito/falla, cookie y encabezados; la integración web queda en HU-033.
- [x] **T-04 — Probar acceso.** Dificultad: Alto. Cubrir credenciales válidas e inválidas y roles.
## Criterios de aceptación
### CA-01 — Sesión válida
**Dado** email y contraseña válidos, **cuando** se inicia sesión, **entonces** se emiten access y refresh separados y el contexto incluye el rol.
### CA-02 — Sesión inválida
**Dado** una credencial inválida, **cuando** se inicia sesión, **entonces** se rechaza sin emitir tokens ni revelar cuál dato falló.
### CA-03 — Acceso por rol
**Dado** una sesión emitida, **cuando** se intenta una capacidad restringida, **entonces** la autorización usa el rol presente en su contexto.
### CA-04 — Contrato y error de autenticación
**Dado** un login válido, **cuando** responde la API, **entonces** entrega access en JSON y refresh en cookie HttpOnly; ante email desconocido o contraseña incorrecta responde el mismo error sin tokens.
## Definition of Done
- [x] CA-01 a CA-04 validados con pruebas de seguridad/REST; integración de cliente diferida a HU-033.
- [x] Access/refresh están separados, configurables y ausentes de logs.
- [x] La documentación de contrato/seguridad y Scrum está actualizada.
- [x] CA-04, expiración/tipo de JWT, rechazo por rol y ausencia de tokens en logs están probados; `mvn test` pasa.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `LoginIntegrationTest.ca01_ca04_issuesSeparateAccessInJsonAndRefreshInHttpOnlyCookieWithRoles`; `JwtTokenService.issue` | Access en JSON con `typ=access` y `roles`; refresh distinto solo en cookie. |
| CA-02 | Cumple | `LoginIntegrationTest.ca02_ca04_unknownEmailAndWrongPasswordGetTheSameErrorWithoutTokens`, `ca02_inactiveAccountCannotSignIn` | Mismo 401 y cuerpo, sin cookie; cuenta inactiva rechazada. |
| CA-03 | Cumple | `LoginIntegrationTest.ca03_authorizationUsesTheRoleInTheSessionContext`, `ca03_tamperedOrRefreshTokenIsNotAcceptedAsAccess` | Sin token 401, USER 403 en ADMIN, ADMIN 200; payload alterado a ADMIN o refresh usado como access → 401. |
| CA-04 | Cumple | `AuthController.login`; prueba CA-01 | Cookie `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax` en local (`None; Secure` con `COOKIE_SECURE=true`); access de 900 s. |
| DoD pruebas | Cumple | `LoginIntegrationTest` (5), `AuthRequestGuardIntegrationTest` (5), `auth.interceptor.spec.ts` (6) | Backend en MySQL real; cliente Angular 11/11. |
| DoD tokens/config/logs | Cumple | `JwtTokenService` (rechaza secretos iguales o < 32 bytes), `application.yml`, `AuthRequestGuard` | Secretos y duraciones por entorno; no hay logger de tokens. |
| DoD documentación | Cumple | `contracts.md`, esta HU, `traceability.md` | La UI envía credenciales y mantiene el access solamente en memoria. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-17 — Corte backend aprobado, validado y completado con `mvn test` (8/8); tareas de UI movidas a HU-033.
- 2026-09-24 — Fase 1 conectó inicio de sesión Angular al contrato real y eliminó el selector simulado de roles. Estado `Completada`.
- 2026-10-01 — Revalidación con pruebas reescritas. RED real en `ca01_ca04_…` (login con email con espacios → 400); corregido normalizando en el dominio. Se mantiene `Completada`.
## Notas y decisiones
- La ubicación/gestión concreta de tokens debe respetar el contrato de seguridad aprobado.
- 2026-09-17: usuario aprobó el corte backend y cookie cross-site. Estado visual y almacenamiento de access por el cliente pasan a HU-033; no se implementa UI aquí.
