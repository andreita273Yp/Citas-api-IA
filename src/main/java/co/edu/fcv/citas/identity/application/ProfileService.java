package co.edu.fcv.citas.identity.application;

import co.edu.fcv.citas.identity.application.port.in.ProfileUseCase;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.UserAccount;
import co.edu.fcv.citas.shared.application.TransactionPort;

/** HU-010 · Perfil propio: lectura de datos permitidos y cambio exclusivo del teléfono. */
public class ProfileService implements ProfileUseCase {
    private final UserAccountPort users;
    private final TransactionPort tx;

    public ProfileService(UserAccountPort users, TransactionPort tx) {
        this.users = users;
        this.tx = tx;
    }

    @Override
    public UserAccount me(long userId) {
        return users.findActiveById(userId).orElseThrow(IdentityException.InvalidCredentials::new);
    }

    @Override
    public UserAccount updatePhone(long userId, String phone) {
        String valid = PersonalData.phone(phone);
        return tx.inTransaction(() -> {
            me(userId);
            users.changePhone(userId, valid);
            return me(userId);
        });
    }
}
