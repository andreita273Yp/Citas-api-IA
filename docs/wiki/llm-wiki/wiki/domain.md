# Dominio

## HECHO — alcance confirmado

El sistema académico permite a un USER registrarse, autenticarse, gestionar perfil y afiliación, consultar disponibilidad, crear citas, cancelarlas y solicitar reprogramaciones. ADMIN administra catálogos y profesionales, y decide solicitudes especializadas y reprogramaciones. PROFESSIONAL gestiona bloques de disponibilidad y consulta su propia agenda. Fuente: [PRD](../raw/approved/PRD-v1.0.md).

Las citas generales se aprueban automáticamente; las especializadas nacen como solicitud y requieren decisión ADMIN. Las especialidades duran 30 o 60 minutos y una reserva de 60 minutos necesita dos slots consecutivos. Los cambios de estado deben auditarse. Fuente: [PRD](../raw/approved/PRD-v1.0.md).

## HECHO — restricciones de seguridad y datos

Se usan únicamente datos sintéticos. Las contraseñas deben usar hash adaptativo y nunca se registran passwords o tokens. La autorización combina rol y ownership. Fuente: [PRD](../raw/approved/PRD-v1.0.md).

## Relación

Las capacidades relacionales requeridas y sus preguntas pendientes se detallan en [Modelo de datos](data-model.md). La implementación y pruebas se trazan en [Trazabilidad](requirements-traceability.md).
