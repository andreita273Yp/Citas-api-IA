# Registro RED → GREEN

Evidencia de pruebas escritas **antes** de la implementación que fallaron por motivos funcionales (no por sintaxis ni dependencias) y luego pasaron sin debilitarse. Todas se ejecutaron en Docker contra MySQL 8.4 (`<DB_NAME>_test`).

## 2026-10-01 · Paso 1 (S2 · HU-005/HU-006)

**Comando:**

```
docker compose exec -T citas-api-dev mvn -B test -Dtest=RegistrationIntegrationTest,LoginIntegrationTest,SessionLifecycleIntegrationTest
```

**RED (19 pruebas, 3 fallos):**

```
LoginIntegrationTest.ca01_ca04_issuesSeparateAccessInJsonAndRefreshInHttpOnlyCookieWithRoles:46 Status expected:<200> but was:<400>
LoginIntegrationTest.ca03_tamperedOrRefreshTokenIsNotAcceptedAsAccess:99 Status expected:<401> but was:<200>
RegistrationIntegrationTest.ca02_ca04_rejectsSameEmailIgnoringCaseAndSpacesWithoutCreatingAnotherAccount:65 Status expected:<409> but was:<400>
```

**Causas:**

- **Login y registro:** `@Email` validaba antes de normalizar el email (trim y minúsculas), así que un email con espacios recibía 400.
- **Token alterado:** la aserción solo añadía un carácter a la firma. Eso no constituye un ataque, porque no permite falsificar nada; solo cambia la codificación. Se reemplazó por una alteración real del payload (rol ADMIN con la firma original), que es una verificación más estricta.

**GREEN:** con la validación movida al dominio (`EmailAddress.of`), el suite completo dio 31/32. La prueba restante pertenece a HU-008/009.

## 2026-10-01 · Paso 2 (S3-A · HU-014 a HU-017)

**Comando:**

```
docker compose exec -T citas-api-dev mvn -B test -Dtest=SpecialtyAdministrationIntegrationTest,ProfessionalAdministrationIntegrationTest
```

**RED:**

```
Tests run: 7, Failures: 7, Errors: 0 -- in co.edu.fcv.citas.offer.ProfessionalAdministrationIntegrationTest
Tests run: 4, Failures: 4, Errors: 0 -- in co.edu.fcv.citas.offer.SpecialtyAdministrationIntegrationTest
Tests run: 11, Failures: 11, Errors: 0   (todas: Status expected:<201|403|409> but was:<404>)
```

**Causa:** los endpoints `/api/v1/admin/specialties` y `/api/v1/admin/professionals` no existían.

**GREEN:**

```
Tests run: 7, Failures: 0, Errors: 0 -- in ...ProfessionalAdministrationIntegrationTest
Tests run: 4, Failures: 0, Errors: 0 -- in ...SpecialtyAdministrationIntegrationTest
Tests run: 43, Failures: 1   (la restante: IdentityExtensionIntegrationTest.recoveryIsGenericAndTokenIsSingleUse, HU-008/009)
```

**Hallazgo adicional:** durante el GREEN, `ca03_retiringASpecialty…` detectó que reservar una especialidad inactiva lanzaba `EmptyResultDataAccessException` (500). Se corrigió `SchedulingController.duration` para responder 404.
