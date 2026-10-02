---
id: HU-001
tipo: historia-de-usuario
titulo: "Inicializar fundación técnica"
estado: Completada
epica: "[[EP-001-fundacion-y-contrato-del-producto]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 1"
dependencias: []
relacionadas: ["[[HU-002-modelar-persistencia-3fn]]", "[[HU-004-definir-contrato-rest-inicial]]"]
---

# HU-001 — Inicializar fundación técnica
## Historia de usuario
**COMO** equipo de desarrollo  
**QUIERO** disponer de una aplicación base alineada con la arquitectura requerida  
**PARA** construir capacidades posteriores con límites y seguridad verificables.
## Contexto y descripción
El repositorio parte sin aplicación. Esta HU funda backend y cliente elegible sin decidir nombres internos ni framework web.
## Alcance
- Base Java/Spring/Maven hexagonal, configuración externa, seguridad base y cliente TypeScript elegido/aprobado.
## Fuera de alcance
- Lógica funcional de registro, agenda o contrato definitivo.
## Reglas de negocio
- Secretos solo por environment; no datos reales; REST directo sin BFF.
## Dependencias y relaciones
- Épica: [[EP-001-fundacion-y-contrato-del-producto]]
- Dependencias: Ninguna.
- Relacionadas: [[HU-002-modelar-persistencia-3fn]], [[HU-004-definir-contrato-rest-inicial]].
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** establece límites transversales sin implementación existente.
## Tareas de desarrollo
- [x] **T-01 — Inicializar backend requerido.** Dificultad: Alto. Crear estructura que conserve dominio/aplicación independiente de adaptadores.
- [x] **T-02 — Preparar configuración segura.** Dificultad: Medio. Externalizar secretos y habilitar CORS explícito/health recomendado.
- [x] **T-03 — Inicializar cliente TypeScript aprobado.** Dificultad: Medio. Configurar URL de API por environment, sin Express/BFF.
- [x] **T-04 — Añadir verificación base.** Dificultad: Medio. Registrar build/typecheck y pruebas aplicables sin secretos.
## Criterios de aceptación
### CA-01 — Stack verificable
**Dado** el repositorio inicial, **cuando** se inspecciona la configuración, **entonces** se evidencia Java 21, Spring Boot 3.5.x, Maven y cliente TypeScript con framework aprobado.
### CA-02 — Límites arquitectónicos
**Dado** la aplicación base, **cuando** se inspeccionan sus dependencias, **entonces** dominio/aplicación no dependen de HTTP, JPA ni Spring.
### CA-03 — Configuración segura
**Dado** una ejecución de desarrollo, **cuando** se revisan configuración y ejemplos, **entonces** no hay secretos reales y la URL backend es configurable.
## Definition of Done
- [x] CA-01 a CA-03 tienen evidencia de repositorio.
- [x] Hay build/typecheck y pruebas base aplicables con resultado disponible.
- [x] No se introdujo Express/BFF, credenciales ni datos no sintéticos.
- [x] La trazabilidad Scrum está actualizada.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Cumple | `pom.xml` (Java 21, Spring Boot 3.5.6, Maven); `citas-web/package.json` (Angular 21, TypeScript) | Angular elegido a partir del export de AI Studio. |
| CA-02 | Cumple | `FoundationIntegrationTest.ca02_domainAndApplicationLayersDoNotDependOnSpringJpaOrHttp`; `identity/{domain,application}`, `catalog/{domain,application}` | Los casos de uso se ensamblan en `*/config`; la prueba falla si dominio/aplicación importan Spring, JPA, Servlet o Hibernate. |
| CA-03 | Cumple | `application.yml` (secretos solo `${...}`), `.env` ignorado; `citas-web/src/environments/environment.ts` | URL de API configurable por `window.__FCV_CONFIG__`. |
| T-02 | Cumple | `FoundationIntegrationTest.t02_healthIsPublicAndRevealsNoDetails`, `t02_corsAllowsOnlyTheConfiguredFrontendOrigin` | `/actuator/health` público sin detalles; CORS solo `FRONTEND_ORIGIN`. |
| DoD pruebas | Cumple | `mvn test` en Docker; `npm run lint`, `ng test` (11/11), `npm run build` | Sin Express/BFF ni datos reales. |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
- 2026-10-01 — Identidad reestructurada en hexagonal (dominio/aplicación sin Spring, adaptadores JPA/JWT/web), health y CORS estricto probados. Estado `Completada`.
## Notas y decisiones
- 2026-09-24: se seleccionó Angular 21 a partir del export de AI Studio.
- 2026-09-17: aprobado y en desarrollo únicamente el corte backend Java/Spring/Maven, configuración externa, CORS y límites hexagonales. Cliente TypeScript, CA-01 y DoD globales permanecen pendientes; esta HU no se declarará completada con el incremento de identidad.
