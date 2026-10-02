# Riesgos y preguntas abiertas

- Los cambios locales no versionados en ambos repositorios deben preservarse y verificarse antes de cualquier commit.
- Las decisiones de estados, zona horaria y exclusión concurrente se registraron para el núcleo de citas; las transiciones detalladas y el contrato REST por endpoint siguen pendientes de sus HU.
- Debe definirse en HU-019 si una franja comprometida por una cita `REQUESTED`, `APPROVED` o una reprogramación `PENDING` impide editar/eliminar el bloque completo.
- Falta lista completa de catálogos fijos y semillas.
- Falta política de afiliación activa/histórica.
- Los refresh tokens de HU-007 ya tienen rotación, expiración, revocación y almacenamiento definidos; queda pendiente la política futura para gestión multidispositivo.
- El contrato REST de HU-005/006/007 está aprobado; faltan los contratos de las demás HU.
- Angular 21 + TypeScript es el framework frontend detectado; falta evidencia de aprobación visual y cierre cross-repo de HU-033.
- n8n queda fuera del alcance hasta completar S2-S4.
