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

## 2026-10-01 · Paso 3 (S3-B · HU-017 a HU-024)

**Comando:**

```
docker compose exec -T citas-api-dev mvn -B test -Dtest=AgendaBlocksIntegrationTest,BookingIntegrationTest,SpecializedDecisionIntegrationTest
```

**RED (24 pruebas, 23 fallos):**

```
Tests run: 9,  Failures: 8  -- in co.edu.fcv.citas.scheduling.AgendaBlocksIntegrationTest
Tests run: 10, Failures: 10 -- in co.edu.fcv.citas.scheduling.BookingIntegrationTest
Tests run: 5,  Failures: 5  -- in co.edu.fcv.citas.scheduling.SpecializedDecisionIntegrationTest
BookingIntegrationTest.bookingRejectsPastMisalignedAndWrongLocationOrSpecialty:174 Status expected:<400> but was:<200>
BookingIntegrationTest.onlyUsersBookTheirOwnAppointments:188 Status expected:<403> but was:<200>
```

**Causas funcionales:**

- No existían los endpoints de bloques (`/api/v1/professional/blocks`) ni el de decisión (`/api/v1/admin/appointments/{id}/decision`).
- La búsqueda exigía profesional y sede, y no filtraba inicios pasados.
- **Defectos de reglas en el código previo:**
  - Se aceptaba una reserva en el pasado (RN-06).
  - Un PROFESSIONAL podía reservar como paciente.
  - La reserva respondía 200 en lugar de 201.

La única prueba que ya pasaba (`onlyProfessionalsManageBlocks`) se apoya en la regla `/api/v1/professional/** → PROFESSIONAL` agregada en el Paso 2.

**GREEN:**

```
Tests run: 9,  Failures: 0 -- AgendaBlocksIntegrationTest
Tests run: 10, Failures: 0 -- BookingIntegrationTest
Tests run: 7,  Failures: 0 -- domain.SchedulingDomainTest
Tests run: 5,  Failures: 0 -- SpecializedDecisionIntegrationTest
Tests run: 74, Failures: 1   (la restante: recuperación de contraseña, HU-008/009)
```

**Estabilidad de concurrencia:** `hu022_ca02_concurrentBookingsOfTheSameSlotProduceExactlyOneAppointment` y `ca04_concurrentRefreshWithTheSameTokenAllowsExactlyOneRotation` se repitieron 5 veces (`scripts/repeat-concurrency.sh`): 5/5 sin fallos.

**Frontend (Vitest):** `book-appointment.spec.ts` detectó que el aviso "Ese horario acaba de ser tomado" se borraba al recargar la disponibilidad tras un 409. Se corrigió (`search(true)` conserva el aviso). Resultado: 22/22.
