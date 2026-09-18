# Mapa Scrum / Spec-Driven Development

## Propósito y alcance

Este wiki traduce el PRD y las restricciones de arquitectura en unidades pequeñas, secuenciales y verificables. Es un artefacto de planificación: no autoriza implementaciones ni sustituye el contrato REST, el diseño de datos a 3FN, ni las decisiones de arquitectura que deberán quedar enlazadas cuando existan.

Fuentes de trazabilidad: `../PRD.md`, `../RESTRICCIONES_TECNICAS.md` y `../database/REQUISITOS_NORMALIZACION_3FN.md`.

## Estado de las historias

| Estado | Significado | Regla de selección |
|---|---|---|
| Propuesta | Redactada y pendiente de revisión del Product Owner. | No seleccionable. |
| En revisión | Requiere decisión, aclaración o priorización del Product Owner. | No seleccionable. |
| Aprobada | Aceptada explícitamente por el Product Owner. | Seleccionable por S2, S3 o S4 si satisface sus dependencias. |
| En curso | Seleccionada para un incremento. | No se considera terminada. |
| Hecha | Cumple sus criterios y su DoD. | Mantiene evidencia enlazada. |

**Regla vigente:** ninguna HU de este wiki está declarada `Aprobada`. Solo una revisión explícita del usuario puede cambiar ese estado.

## Cómo usar el mapa

1. Revisar [[epicas]] para validar capacidad, alcance y trazabilidad a requisitos.
2. Revisar [[historias]] para aprobar HU individuales en el orden de sus dependencias.
3. Usar [[incrementos]] para seleccionar únicamente HU aprobadas, sin convertir la propuesta en un calendario ni estimarla en horas, días o puntos.
4. Al ejecutar una HU, enlazar desde su sección la especificación, decisión, migración, contrato, pruebas y evidencia que correspondan. No se inventa evidencia antes de implementarla.

## Convenciones

- Identificador: `HU-###`; una historia debe poder verificarse de forma independiente al terminar su secuencia de dependencias.
- Esfuerzo cualitativo: Bajo, Medio, Alto o Muy alto. No es una estimación temporal.
- `DoD común` se suma a la `DoD específica` de cada HU; no la reemplaza.
- Los wikilinks `[[...]]` expresan navegación y dependencias documentales; las dependencias de trabajo están también escritas en cada HU para evitar ambigüedad.

## DoD común aplicable a toda HU

- Criterios de aceptación comprobados y evidencia enlazable desde la HU.
- Diseño alineado con Java 21, Spring Boot 3.5.x, Maven, arquitectura hexagonal, JPA/MySQL/Flyway, REST JSON y seguridad definida, cuando aplique.
- Validación server-side y autorización por rol/ownership cuando la capacidad exponga datos o acciones.
- Sin secretos, tokens, contraseñas ni datos reales en código, documentación, fixtures o logs; los datos son sintéticos.
- Pruebas aplicables desde S3: dominio, aplicación e integración REST/persistencia relevante; y contrato cross-repo para funcionalidades clave si hay cliente web.
- Cambios documentales de contrato, decisiones y trazabilidad actualizados cuando la HU los afecte.

## Navegación

- [[epicas]] — capacidades y cobertura del PRD.
- [[historias]] — backlog atómico con aceptación, DoD, tareas y dependencias.
- [[incrementos]] — propuesta de selección por S2, S3 y S4.
