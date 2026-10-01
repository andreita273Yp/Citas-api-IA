package co.edu.fcv.citas.identity.application.port.in;

import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;

/** Alta de cuentas de personal (p. ej. PROFESSIONAL) por un ADMIN; no hay auto-registro de estos roles. */
public interface CreateStaffAccountUseCase {
    /** Devuelve el id del usuario creado; se une a la transacción del llamador si existe. */
    long create(PersonalData data, String initialPassword, Role role);
}
