# Modelo de datos

## HECHO — capacidades mínimas 3FN

El diseño debe soportar usuarios y múltiples roles; profesionales; relaciones N:M de profesionales con especialidades y sedes; EPS, régimen, plan y afiliación; bloques de disponibilidad; citas de 30/60 minutos; estados e historial; reprogramaciones; y artefactos compatibles con autenticación y recuperación de contraseña. Fuente: [Requisitos de normalización 3FN](../raw/approved/requisitos-normalizacion-3fn.md).

La normalización exige atributos atómicos, relaciones puente para N:M, dependencia completa de claves compuestas y ausencia de dependencias transitivas. No se deben duplicar nombres de EPS, plan, régimen, especialidad o estados como texto divergente. Fuente: [Requisitos de normalización 3FN](../raw/approved/requisitos-normalizacion-3fn.md).

## PREGUNTAS ABIERTAS

- Estrategia de reserva concurrente, retención y liberación de slots.
- Uso de snapshots frente a FKs en citas e historial.
- Afiliación vigente única o historial de afiliaciones.
- Catálogo exacto de estados y transiciones terminales.

No consultar ni usar `database/reference/` hasta que el trainer lo libere. Ver [Riesgos y preguntas](risks-open-questions.md).
