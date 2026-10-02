package co.edu.fcv.citas.identity.application;

import co.edu.fcv.citas.identity.application.port.in.CreateStaffAccountUseCase;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.PasswordPolicy;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;
import co.edu.fcv.citas.shared.application.TransactionPort;

/** Crea la identidad de un miembro del personal con las mismas reglas de unicidad y contraseña que el registro. */
public class StaffAccountService implements CreateStaffAccountUseCase {
    private final UserAccountPort users;
    private final PasswordHasher passwords;
    private final TransactionPort tx;

    public StaffAccountService(UserAccountPort users, PasswordHasher passwords, TransactionPort tx) {
        this.users = users;
        this.passwords = passwords;
        this.tx = tx;
    }

    @Override
    public long create(PersonalData data, String initialPassword, Role role) {
        if (role == Role.USER) throw new IdentityException.InvalidData("Las cuentas USER se crean por autoregistro");
        PasswordPolicy.check(initialPassword);
        return tx.inTransaction(() -> {
            if (users.existsByEmailOrDocument(data)) throw new IdentityException.Duplicate();
            return users.create(data, passwords.hash(initialPassword), role).id();
        });
    }
}
