---
id: HU-033
tipo: historia-de-usuario
titulo: "Integrar cliente web con API"
estado: Pendiente de aprobación
epica: "[[EP-008-cliente-web-y-automatizaciones-posteriores]]"
esfuerzo: Alto
sprint_sugerido: "Incremento 7"
dependencias: ["[[HU-004-definir-contrato-rest-inicial]]", "[[HU-032-consultar-auditoria-de-estados]]"]
relacionadas: []
---
# HU-033 — Integrar cliente web con API
## Historia de usuario
**COMO** usuario de cualquiera de los roles  
**QUIERO** acceder por web a las pantallas obligatorias conectadas directamente a la API  
**PARA** completar los flujos aprobados del producto.
## Contexto y descripción
La estética se deriva de Stitch/AI Studio; el framework será React o Angular, aún pendiente. Esta HU consolida integración, no duplica reglas backend.
## Alcance
- Rutas/pantallas obligatorias de registro, sesión, perfil, disponibilidad/citas, dashboards profesional y ADMIN, y CRUD ADMIN mediante contrato aprobado.
## Fuera de alcance
- Elegir framework sin aprobación, Express/BFF o inventar comportamiento no disponible en API.
## Reglas de negocio
- REST directo a `citas-api`; URL por environment; CORS explícito; UI respeta roles/ownership del backend.
## Dependencias y relaciones
- Épica: [[EP-008-cliente-web-y-automatizaciones-posteriores]]
- Dependencias: [[HU-004-definir-contrato-rest-inicial]], [[HU-032-consultar-auditoria-de-estados]].
- Relacionadas: Ninguna.
## Esfuerzo
**Nivel:** Alto. **Justificación de dificultad:** integra múltiples flujos/roles y depende de decisión visual/framework.
## Tareas de desarrollo
- [ ] **T-01 — Acordar framework/diseño.** Dificultad: Alto. Seguir Stitch → aprobación → AI Studio antes de implementar UI.
- [ ] **T-02 — Implementar navegación/estado por rol.** Dificultad: Alto. Cubrir pantallas obligatorias y sesión.
- [ ] **T-03 — Conectar cliente REST.** Dificultad: Alto. Usar contrato versionado, errores y URL de environment, sin BFF.
- [ ] **T-04 — Verificar cross-repo.** Dificultad: Alto. Ejecutar build/typecheck/pruebas y flujos clave contra API.
## Criterios de aceptación
### CA-01 — Pantallas obligatorias
**Dado** el diseño y las HU aprobadas, **cuando** cada rol navega, **entonces** puede llegar a las pantallas obligatorias pertinentes del PRD.
### CA-02 — Consumo directo
**Dado** una acción web de una HU aprobada, **cuando** se ejecuta, **entonces** el cliente llama directamente a `citas-api` por REST/JSON usando URL configurable.
### CA-03 — Manejo de seguridad
**Dado** una respuesta de autorización/validación, **cuando** el cliente la recibe, **entonces** muestra un resultado acorde al contrato sin exponer tokens o secretos.
## Definition of Done
- [ ] CA-01 a CA-03 validados para los flujos aprobados en ambos repositorios.
- [ ] Framework y diseño visual aprobados; build/typecheck y pruebas aplicables con evidencia.
- [ ] No existe Express/BFF; contrato/cors/environment y Scrum están actualizados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Pendiente | — | Requiere diseño/framework. |
| CA-02 | Pendiente | — | — |
| CA-03 / DoD | Pendiente | — | — |
## Historial de validación
- 2026-09-17 — HU creada en estado `Pendiente de aprobación`.
## Notas y decisiones
- React o Angular es una decisión abierta; no se toma en esta HU.
- 2026-09-17: aquí quedan las tareas visuales diferidas de HU-005/006/007: formulario de registro, feedback de login, renovación desde navegador y limpieza de estado autenticado al salir. Integrar `credentials`, `X-Requested-With` y el contrato de cookie cuando se aborde la UI.
