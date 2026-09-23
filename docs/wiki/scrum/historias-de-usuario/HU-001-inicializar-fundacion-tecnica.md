---
id: HU-001
tipo: historia-de-usuario
titulo: "Inicializar fundación técnica"
estado: En desarrollo
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
- [ ] **T-01 — Inicializar backend requerido.** Dificultad: Alto. Crear estructura que conserve dominio/aplicación independiente de adaptadores.
- [ ] **T-02 — Preparar configuración segura.** Dificultad: Medio. Externalizar secretos y habilitar CORS explícito/health recomendado.
- [ ] **T-03 — Inicializar cliente TypeScript aprobado.** Dificultad: Medio. Configurar URL de API por environment, sin Express/BFF.
- [ ] **T-04 — Añadir verificación base.** Dificultad: Medio. Registrar build/typecheck y pruebas aplicables sin secretos.
## Criterios de aceptación
### CA-01 — Stack verificable
**Dado** el repositorio inicial, **cuando** se inspecciona la configuración, **entonces** se evidencia Java 21, Spring Boot 3.5.x, Maven y cliente TypeScript con framework aprobado.
### CA-02 — Límites arquitectónicos
**Dado** la aplicación base, **cuando** se inspeccionan sus dependencias, **entonces** dominio/aplicación no dependen de HTTP, JPA ni Spring.
### CA-03 — Configuración segura
**Dado** una ejecución de desarrollo, **cuando** se revisan configuración y ejemplos, **entonces** no hay secretos reales y la URL backend es configurable.
## Definition of Done
- [ ] CA-01 a CA-03 tienen evidencia de repositorio.
- [ ] Hay build/typecheck y pruebas base aplicables con resultado disponible.
- [ ] No se introdujo Express/BFF, credenciales ni datos no sintéticos.
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
- React o Angular sigue pendiente de selección a partir de Stitch/AI Studio.
- 2026-09-17: aprobado y en desarrollo únicamente el corte backend Java/Spring/Maven, configuración externa, CORS y límites hexagonales. Cliente TypeScript, CA-01 y DoD globales permanecen pendientes; esta HU no se declarará completada con el incremento de identidad.
