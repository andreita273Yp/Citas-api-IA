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

## 2026-10-01 · Paso 4 (S4-A · HU-008 a HU-013)

**Comando:**

```
docker compose exec -T citas-api-dev mvn -B test -Dtest=PasswordRecoveryIntegrationTest,ProfileAndAffiliationIntegrationTest,InsuranceCatalogAdministrationIntegrationTest
```

**RED (18 pruebas, 14 fallos):**

```
Tests run: 6, Failures: 6 -- PasswordRecoveryIntegrationTest  (latestToken: Status expected:<200> but was:<404>)
Tests run: 5, Failures: 4 -- InsuranceCatalogAdministrationIntegrationTest  (Status expected:<201> but was:<200>)
Tests run: 7, Failures: 4 -- ProfileAndAffiliationIntegrationTest
ProfileAndAffiliationIntegrationTest.hu010_ca02_ca03_onlyThePhoneCanBeUpdatedAndItIsValidated:77 Status expected:<400> but was:<200>
ProfileAndAffiliationIntegrationTest.hu011_ca01_userAssociatesAValidPlanAndGetsEpsAndRegimeFromIt:86 Status expected:<204> but was:<200>
```

**Causas funcionales:**

- La recuperación respondía 202 sin generar ni entregar token, y no existían el buzón local ni el restablecimiento.
- El teléfono aceptaba cualquier texto ("abc").
- La afiliación exigía `regimeCode` aparte, lo que duplicaba el régimen del plan.
- Un plan nuevo tomaba el primer régimen de la lista.
- Las altas respondían 200 en lugar de 201.

**GREEN:**

```
Tests run: 6, Failures: 0 -- PasswordRecoveryIntegrationTest
Tests run: 5, Failures: 0 -- InsuranceCatalogAdministrationIntegrationTest
Tests run: 7, Failures: 0 -- ProfileAndAffiliationIntegrationTest
Tests run: 89, Failures: 0, Errors: 0   (suite completa en verde por primera vez)
```

## 2026-10-01 · Paso 5 (S4-B · HU-025 a HU-028)

**Comando:**

```
docker compose exec -T citas-api-dev mvn -B test -Dtest=AppointmentLifecycleIntegrationTest
```

**RED (11 pruebas, 8 fallos):**

```
hu025_ca01_ca02_…:73 No matching value at JSON path "$[?(@.id == 1)].locationCode"
hu027_ca01_ca02_…:136 Status expected:<201> but was:<200>
hu028_ca01_…, hu028_ca02_…, hu028_ca03_…, hu028_afterRejection… → requestReschedule: Status expected:<201> but was:<200>
cancellingWithAPendingReschedule… → requestReschedule: Status expected:<201> but was:<200>
```

**Causas funcionales:**

- La vista de la cita no traía código de sede ni ids de profesional y especialidad.
- La solicitud de reprogramación respondía 200 sin estado visible en la cita.
- No existía la elección del paciente tras un rechazo (RF-15).

Las 3 pruebas que pasaban ya en RED (detalle ajeno, cancelación básica y restricciones de cancelación) cubrían comportamiento existente.

**GREEN:**

```
Tests run: 11,  Failures: 0 -- AppointmentLifecycleIntegrationTest
Tests run: 100, Failures: 0, Errors: 0
```