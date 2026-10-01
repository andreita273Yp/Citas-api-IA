package co.edu.fcv.citas.identity.application.port.in;

import co.edu.fcv.citas.identity.domain.UserAccount;

/** HU-005 · Un visitante crea su propia cuenta USER. */
public interface RegisterUserUseCase {
    UserAccount register(Command command);

    /** {@code insurancePlanId} es opcional (afiliación inicial, HU-011). */
    record Command(String firstName, String lastName, String documentType, String documentNumber, String email,
                   String phone, String password, Long insurancePlanId) { }
}
