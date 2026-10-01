# Línea base — Fase 0

## HECHO — 2026-09-24

Los repositorios existentes se preservaron sin limpiar, restaurar ni sobrescribir archivos locales. Se creó la rama local `develop` en `citas-api` y `citas-web`; `main` permanece como referencia estable.

## Entorno reproducible

- Backend: contenedor `citas-api-dev` con Maven y volumen de caché.
- Frontend: contenedor `citas-web-dev` con el volumen Linux `web_node_modules` en `/workspace/node_modules`.
- El volumen del frontend evita cargar binarios nativos de Windows dentro de Alpine Linux. Las dependencias se instalan mediante `npm ci` dentro del contenedor.

## Validación observada

| Comando | Resultado |
|---|---|
| `docker compose exec -T citas-api-dev mvn test` | PASS — 5 pruebas, 0 fallos |
| `docker compose exec -T citas-web-dev npm run lint` | PASS |
| `docker compose exec -T citas-web-dev npx ng test --watch=false` | PASS — 4 pruebas, 0 fallos |
| `docker compose exec -T citas-web-dev npm run build` | PASS |

## Límite del corte

Esta fase no crea catálogos, migraciones, endpoints de negocio, disponibilidad, citas, reprogramaciones ni integración n8n. Esos cambios requieren la aprobación explícita de la fase siguiente.
