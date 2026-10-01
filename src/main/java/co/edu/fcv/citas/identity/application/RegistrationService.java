package co.edu.fcv.citas.identity.application;

import co.edu.fcv.citas.identity.application.port.in.RegisterUserUseCase;
import co.edu.fcv.citas.identity.application.port.out.InsuranceAffiliationPort;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.TransactionPort;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.Document;
import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.PasswordPolicy;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;
import co.edu.fcv.citas.identity.domain.UserAccount;

/** HU-005 · Autoregistro: solo crea cuentas USER, con email y documento únicos y contraseña en BCrypt. */
public class RegistrationService implements RegisterUserUseCase {
    private final UserAccountPort users;
    private final PasswordHasher passwords;
    private final InsuranceAffiliationPort affiliations;
    private final TransactionPort tx;

    public RegistrationService(UserAccountPort users, PasswordHasher passwords, InsuranceAffiliationPort affiliations,
                               TransactionPort tx) {
        this.users = users;
        this.passwords = passwords;
        this.affiliations = affiliations;
        this.tx = tx;
    }

    @Override
    public UserAccount register(Command c) {
        PersonalData data = new PersonalData(c.firstName(), c.lastName(), new Document(c.documentType(), c.documentNumber()),
                EmailAddress.of(c.email()), c.phone());
        PasswordPolicy.check(c.password());
        return tx.inTransaction(() -> {
            if (users.existsByEmailOrDocument(data)) throw new IdentityException.Duplicate();
            if (c.insurancePlanId() != null && !affiliations.isActivePlan(c.insurancePlanId()))
                throw new IdentityException.InvalidData("Plan de afiliación inexistente o inactivo");
            UserAccount created = users.create(data, passwords.hash(c.password()), Role.USER);
            if (c.insurancePlanId() != null) affiliations.affiliate(created.id(), c.insurancePlanId());
            return created;
        });
    }
}
