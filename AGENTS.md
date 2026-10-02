# citas-api — instrucciones del agente principal

## Estado observado del repositorio

Existe una línea base Spring Boot con `pom.xml`, código de identidad/autenticación y `V1__identity.sql`. Las HU Scrum existen, pero una HU pendiente de aprobación no autoriza implementación funcional. El contrato vigente cubre autenticación; los contratos de citas se aprueban antes de codificarse.

Preserva los cambios locales existentes y no abras ni imprimas secretos de `.env`.

## Fuentes y orden de consulta

Antes de un cambio funcional, leer:

1. `../PRD.md`.
2. `../RESTRICCIONES_TECNICAS.md`.
3. `../database/REQUISITOS_NORMALIZACION_3FN.md` si el cambio toca datos.
4. La HU y su DoD aprobados en `docs/wiki/scrum/historias-de-usuario/`.
5. `docs/wiki/llm-wiki/wiki/index.md` como contexto global, si existe.

El PRD y las restricciones prevalecen sobre inferencias. No consultar `../database/reference/` hasta autorización del trainer.

## Alcance de este agente

Este repositorio implementa el backend del laboratorio: Java 21, Spring Boot 3.5.x, Maven, REST/JSON, Spring Security con JWT access/refresh, MySQL 8.4, Spring Data JPA, Flyway, reglas de negocio y pruebas backend.

No editar `../citas-web`. No acoplar el backend a React o Angular; el único límite compartido es el contrato REST documentado y aprobado. No crear Express ni BFF.

La LLM Wiki es global y la mantiene el agente orquestador. Este agente puede consultarla, pero no crea ni mantiene una wiki propia ni modifica `docs/wiki/llm-wiki/` salvo instrucción explícita del orquestador.

## Arquitectura obligatoria para los cambios posteriores

- El dominio no depende de Spring, JPA, HTTP ni clases de adaptadores.
- Los casos de uso y la coordinación de reglas viven en aplicación.
- Los puertos expresan dependencias de entrada y salida.
- REST/HTTP y persistencia JPA/MySQL son adaptadores; los controladores traducen HTTP y no concentran lógica de negocio.
- Definir paquetes y nombres solo después de inicializar el proyecto; preservar estas dependencias aunque cambie la estructura concreta.
- Mantener las reglas de disponibilidad, reserva, transiciones, autorización y auditoría verificables mediante casos de uso y pruebas.

## Datos y seguridad

- Todo cambio de esquema requiere migración Flyway versionada y una justificación basada en 3FN, claves, cardinalidades o dependencias funcionales afectadas.
- No modificar esquema manualmente ni depender de DDL automático como sustituto de Flyway.
- Los catálogos fijos se cargan mediante seed; los datos del laboratorio son sintéticos.
- Secretos solo por variables de entorno. Nunca registrar, imprimir, versionar ni devolver passwords, JWT, refresh tokens, secretos de DB u OAuth.
- Aplicar hash adaptativo a passwords, validación server-side, CORS explícito y autorización por rol y ownership.
- Mantener access y refresh token como artefactos separados, con refresh, revocación/logout y recuperación de contraseña de un solo uso conforme al PRD.

## Flujo de trabajo por HU

1. Localizar la HU aprobada y su DoD. Si no existen, detener la implementación funcional y solicitar su creación/aprobación al flujo Scrum.
2. Identificar RF/RN del PRD, roles, ownership, datos, transiciones y contrato REST afectados.
3. Proponer un plan antes de editar, listando archivos, migraciones, endpoints, pruebas y cualquier impacto de contrato.
4. Implementar el mínimo coherente dentro de los límites hexagonales.
5. Ejecutar las pruebas relevantes: dominio y aplicación para reglas; integración REST/persistencia para adaptadores, seguridad o migraciones.
6. Verificar arquitectura y DoD, incluyendo que controladores no contengan negocio y que el dominio no importe frameworks.
7. Resumir evidencia, comandos ejecutados y elementos no verificados. Si se altera REST, comunicar el cambio al orquestador para coordinación con `citas-web`.

## Reglas del dominio que requieren especial atención

- No permitir cita ni retención sobre slots ocupados; una duración de 60 minutos requiere dos slots consecutivos.
- Las citas generales se aprueban automáticamente; las especializadas requieren decisión ADMIN y un rechazo requiere motivo.
- No permitir bloques ni citas en el pasado, solapamiento de bloques, ni agenda de un profesional en sede no asignada.
- Cancelación o rechazo libera las reservas correspondientes; una reprogramación pendiente conserva la cita y franja originales hasta aprobación.
- Todo cambio de estado debe ser explícito, autorizado y auditado; los datos de auditoría no se exponen a CRUD normal.

No resolver por inferencia el catálogo final de estados, expiración de retenciones, semántica de Medicina General, política de snapshots, zona horaria ni los detalles de endpoints/payloads: registrarlos como decisiones o preguntas para el orquestador antes de codificarlos.

## Git y verificación

`main` es estable y `develop` es trabajo. No reescribir historial para ocultar avance.

Usar el contenedor `citas-api-dev` cuando Maven no esté disponible en el host. Mantener `mvn test` relevante y no afirmar validación de build, integración o seguridad sin evidencia reproducible.
