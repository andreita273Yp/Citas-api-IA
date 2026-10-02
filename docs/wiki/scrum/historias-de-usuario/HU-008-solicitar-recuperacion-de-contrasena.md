---
id: HU-008
tipo: historia-de-usuario
titulo: "Solicitar recuperación de contraseña"
estado: Completada
epica: "[[EP-002-identidad-y-perfil-del-usuario]]"
esfuerzo: Medio
sprint_sugerido: "Incremento 2"
dependencias: ["[[HU-005-registrar-usuario]]", "[[HU-004-definir-contrato-rest-inicial]]"]
relacionadas: ["[[HU-009-restablecer-contrasena]]"]
---

# HU-008 — Solicitar recuperación de contraseña
## Historia de usuario
**COMO** usuario que no recuerda su contraseña  
**QUIERO** solicitar un token temporal de recuperación por email  
**PARA** restablecer mi acceso de forma segura.
## Contexto y descripción
SMTP real es opcional; en desarrollo el token solo puede exponerse por vía segura controlada.
## Alcance
- Solicitud por email, emisión temporal/de único uso y canal controlado de desarrollo aprobado.
## Fuera de alcance
- SMTP obligatorio, SMS/WhatsApp o revelar tokens en logs públicos.
## Reglas de negocio
- Token temporal y de único uso; secretos/tokens no deben registrarse.
## Dependencias y relaciones
- Épica: [[EP-002-identidad-y-perfil-del-usuario]]
- Dependencias: [[HU-005-registrar-usuario]], [[HU-004-definir-contrato-rest-inicial]].
- Relacionadas: [[HU-009-restablecer-contrasena]].
## Esfuerzo
**Nivel:** Medio. **Justificación de dificultad:** exige token seguro y una decisión de canal de desarrollo.
## Tareas de desarrollo
- [x] **T-01 — Definir token temporal.** Dificultad: Alto. Establecer expiración, consumo y almacenamiento seguro.
- [x] **T-02 — Diseñar solicitud y respuesta segura.** Dificultad: Medio. No filtrar datos o token sin mecanismo aprobado.
- [x] **T-03 — Integrar pantalla/feedback.** Dificultad: Bajo. Informar resultado sin revelar información sensible.
- [x] **T-04 — Probar ciclo de emisión.** Dificultad: Medio. Cubrir email existente/no existente sin enumeración indebida.
## Criterios de aceptación
### CA-01 — Token temporal
**Dado** una solicitud válida, **cuando** se procesa, **entonces** se genera un token temporal asociado al usuario y apto para un solo uso.
### CA-02 — Respuesta segura
**Dado** cualquier email recibido, **cuando** se solicita recuperación, **entonces** la respuesta no expone credenciales, tokens ni información innecesaria de existencia de cuenta.
### CA-03 — Desarrollo controlado
**Dado** que no hay SMTP real, **cuando** se ejecuta en desarrollo, **entonces** el token solo se entrega mediante mecanismo seguro previamente aprobado.
## Definition of Done
- [x] CA-01 a CA-03 tienen pruebas/evidencia y el canal de desarrollo está documentado.
- [x] Tokens no aparecen en logs ni repositorio; esquema/migración aplicable está verificado.
- [x] Trazabilidad Scrum actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `PasswordRecoveryIntegrationTest.hu008_ca01_ca02_requestIsGenericAndCreatesATemporarySingleUseTokenStoredOnlyAsHash`, `hu008_aNewRequestInvalidatesThePreviousToken`; `PasswordRecoveryService` | Token aleatorio de 256 bits, vence en 30 min, un solo uso; una solicitud nueva invalida la anterior. |
| CA-02 | Cumple | Misma prueba | `202` idéntico exista o no la cuenta; la respuesta no incluye el token; en BD solo el hash SHA-256 (64 hex). |
| CA-03 / DoD | Cumple | `hu008_ca03_localMailboxIsTheOnlyDeliveryChannelAndIsAdminOnly`; `LocalMailbox` (`app.recovery.local-mailbox`) | Sin SMTP: buzón local en memoria, solo ADMIN (USER 403, sin sesión 401) y solo si está habilitado; si se desactiva, el endpoint no existe y el token no se entrega. Nunca se registra en logs. `citas-web` `AdminInsurance` lo muestra. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-09-29 — Implementado ciclo de solicitud genérica `202`, token aleatorio guardado como hash y buzón exclusivo de perfil `local`/ADMIN. Prueba de integración en verde; pendiente recorrido manual.
- 2026-10-01 — Revalidación 2026-10-01: el historial anterior afirmaba la implementación, pero el endpoint de recuperación respondía 202 sin generar token y la prueba citada fallaba. RED: 14 de 18 pruebas nuevas fallaron (incluido teléfono "abc" aceptado); GREEN tras el módulo `insurance` y la recuperación hexagonal. Estado `Completada`.
## Notas y decisiones
- Incógnita abierta: mecanismo seguro de entrega local.
