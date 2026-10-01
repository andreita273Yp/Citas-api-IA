package co.edu.fcv.citas.identity.application.port.out;

import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;
import co.edu.fcv.citas.identity.domain.UserAccount;
import java.util.Optional;

/** Persistencia de cuentas de usuario. */
public interface UserAccountPort {
    boolean existsByEmailOrDocument(PersonalData data);

    /** Crea la cuenta; lanza {@code IdentityException.Duplicate} si la base detecta unicidad violada. */
    UserAccount create(PersonalData data, String passwordHash, Role role);

    Optional<UserAccount> findActiveByEmail(EmailAddress email);

    Optional<UserAccount> findActiveById(long id);
}
