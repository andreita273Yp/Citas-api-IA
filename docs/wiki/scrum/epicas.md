# Épicas funcionales y trazabilidad

Todas las épicas empiezan en estado `Propuesta`; sus HU tampoco están aprobadas. Las referencias `RF`, `RN` y `3FN` remiten exclusivamente a las fuentes autorizadas descritas en [[README]].

| Épica | Capacidad funcional | Requisitos cubiertos | Historias | Dependencia principal |
|---|---|---|---|---|
| [[historias#e01-identidad-y-acceso|E01 Identidad y acceso]] | Identificar, autenticar y recuperar acceso de actores. | RF-01–03; seguridad mínima; 3FN: usuarios/roles/tokens. | HU-001–HU-006 | Base de datos y contratos de identidad. |
| [[historias#e02-perfil-y-catalogos|E02 Perfil y catálogos]] | Mantener perfil, afiliación y catálogos del dominio. | RF-04–06; 3FN: catálogos/afiliación. | HU-007–HU-012 | E01 para acciones USER/ADMIN. |
| [[historias#e03-profesionales-y-disponibilidad|E03 Profesionales y disponibilidad]] | Habilitar oferta asistencial y agenda publicable. | RF-07–10; RN-05–08; 3FN: N:M, bloques, slots. | HU-013–HU-019 | E02 y reglas de disponibilidad. |
| [[historias#e04-citas-y-ciclo-de-vida|E04 Citas y ciclo de vida]] | Reservar, administrar y cerrar citas. | RF-11–14, 16–19; RN-01–04, RN-09, RN-11–12. | HU-020–HU-029 | E03 y política transaccional de slots. |
| [[historias#e05-reprogramacion|E05 Reprogramación segura]] | Cambiar una cita aprobada sin perder su franja original. | RF-15, RF-18–19; RN-01, RN-04, RN-09–11; 3FN: solicitud/retenido. | HU-030–HU-033 | E04, especialmente HU-024 y HU-025. |
| [[historias#e06-experiencia-web-y-contrato|E06 Experiencia web y contrato]] | Permitir que cada actor complete los flujos mediante REST directo. | RF-20 y pantallas obligatorias; restricciones frontend. | HU-034–HU-038 | Contratos estables de E01–E05 y diseño visual aprobado. |

## Reglas transversales que no se degradan a tareas opcionales

| Tema | Historias que deben considerarlo |
|---|---|
| 3FN: atributos atómicos, puentes N:M, catálogos por FK, sin dependencias transitivas | HU-001, HU-007–HU-012, HU-013–HU-019, HU-020–HU-033 |
| No doble reserva / slots consecutivos | HU-017–HU-019, HU-020–HU-025, HU-030–HU-033 |
| Auditoría inmutable de cambios | HU-022, HU-024–HU-029, HU-031–HU-033 |
| Autorización por rol y ownership | HU-002–HU-006, HU-007–HU-038 |
| Datos y credenciales seguros | Todas las HU; atención reforzada HU-001–HU-006 y HU-034–HU-038 |
| Contrato REST documentado | HU-006, HU-012, HU-019, HU-029, HU-033, HU-034–HU-038 |

## Fuera de este backlog inicial

Historia clínica, pagos, facturación real, datos reales, SMS/WhatsApp, SMTP obligatorio, CI/CD obligatorio e integración n8n pertenecen al alcance excluido o posterior del PRD. S5/S6 podrán crear épicas nuevas de automatización sin alterar el núcleo documentado aquí.
