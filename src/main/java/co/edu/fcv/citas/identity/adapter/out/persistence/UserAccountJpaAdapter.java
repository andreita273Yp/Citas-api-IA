package co.edu.fcv.citas.identity.adapter.out.persistence;

import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import co.edu.fcv.citas.identity.domain.Document;
import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;
import co.edu.fcv.citas.identity.domain.UserAccount;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class UserAccountJpaAdapter implements UserAccountPort {
    private final UserJpaRepository users;
    private final RoleJpaRepository roles;

    UserAccountJpaAdapter(UserJpaRepository users, RoleJpaRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public boolean existsByEmailOrDocument(PersonalData data) {
        return users.existsByEmailOrDocumentTypeAndDocumentNumber(data.email().value(), data.document().type(), data.document().number());
    }

    @Override
    public UserAccount create(PersonalData data, String passwordHash, Role role) {
        RoleEntity roleEntity = roles.findByCode(role.name()).orElseThrow(() -> new IllegalStateException("Rol no sembrado: " + role));
        try {
            return toDomain(users.saveAndFlush(new UserEntity(data.firstName(), data.lastName(), data.document().type(),
                    data.document().number(), data.email().value(), data.phone(), passwordHash, roleEntity)));
        } catch (DataIntegrityViolationException e) {
            // Carrera entre dos registros: las restricciones únicas de la base son la última barrera.
            throw new IdentityException.Duplicate();
        }
    }

    @Override
    public Optional<UserAccount> findActiveByEmail(EmailAddress email) {
        return users.findByEmailAndActiveTrue(email.value()).map(UserAccountJpaAdapter::toDomain);
    }

    @Override
    public Optional<UserAccount> findActiveById(long id) {
        return users.findByIdAndActiveTrue(id).map(UserAccountJpaAdapter::toDomain);
    }

    @Override
    public void changePassword(long userId, String passwordHash) {
        users.findById(userId).ifPresent(u -> {
            u.changePasswordHash(passwordHash);
            users.flush();
        });
    }

    @Override
    public void changePhone(long userId, String phone) {
        users.findById(userId).ifPresent(u -> {
            u.changePhone(phone);
            users.flush();
        });
    }

    private static UserAccount toDomain(UserEntity e) {
        PersonalData personal = new PersonalData(e.firstName(), e.lastName(), new Document(e.documentType(), e.documentNumber()),
                EmailAddress.of(e.email()), e.phone());
        return new UserAccount(e.id(), personal, e.passwordHash(), e.active(),
                e.roles().stream().map(r -> Role.valueOf(r.code())).collect(Collectors.toSet()));
    }
}
