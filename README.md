# citas-api

Backend Java 21 con Spring Boot 3.5.x, Maven, Spring Security/JWT, JPA, Flyway y MySQL, en arquitectura hexagonal. Módulos `identity`, `catalog`, `offer`, `insurance` y `scheduling`, con HU-001 a HU-032 implementadas.

## Verificación

Desde la raíz del workspace, con `docker compose up -d`:

```powershell
docker compose exec -T citas-api-dev mvn -B test                              # suite completa sobre MySQL real (<DB_NAME>_test)
powershell -ExecutionPolicy Bypass -File citas-api\scripts\smoke\run-smoke.ps1  # prueba REST contra la app; recrea la base demo
```

**Hook pre-commit (S3 · V8):** se activa una vez por clon con `git config core.hooksPath .githooks`.

- **En cada commit:** bloquea secretos y archivos `.env`.
- **Si el commit toca `src/` o `pom.xml`:** ejecuta la suite backend en Docker.
- **`HOOK_FULL=1 git commit …`:** fuerza todas las validaciones.

El marcador de la demostración (`FAKE_SECRET_FOR_S3_TEST_ONLY`) solo puede aparecer en la documentación (`.githooks/secret-allowlist`).

## Convenciones vigentes

- `V1__identity.sql` se preserva; los cambios de datos usan migraciones Flyway posteriores.
- El dominio y los casos de uso no dependen de HTTP, JPA ni Spring; REST y persistencia son adaptadores.
- Los secretos se inyectan por variables de entorno; no se registran ni versionan.
- La zona de negocio y los contratos posteriores se documentan en la Wiki antes de implementar el núcleo de citas.

## Documentación compartida
- `docs/wiki/scrum/`: épicas/HU generadas con la Skill Scrum.
- `docs/wiki/llm-wiki/`: única LLM Wiki global del workspace.
- `automations/n8n/`: JSON exportados en S5/S6.

Lee el PRD, las restricciones, la HU aprobada y la Wiki antes de modificar una capacidad funcional.
