package co.edu.fcv.citas.identity.application.port.in;

import co.edu.fcv.citas.identity.domain.UserAccount;

/** HU-010 · El usuario consulta su perfil y solo puede actualizar su teléfono. */
public interface ProfileUseCase {
    UserAccount me(long userId);

    UserAccount updatePhone(long userId, String phone);
}
