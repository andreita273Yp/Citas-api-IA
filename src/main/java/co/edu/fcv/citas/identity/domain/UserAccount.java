package co.edu.fcv.citas.identity.domain;

import java.util.Set;

/** Cuenta de usuario persistida; {@code passwordHash} nunca sale de la capa de aplicación. */
public record UserAccount(long id, PersonalData personal, String passwordHash, boolean active, Set<Role> roles) {
    public UserAccount {
        roles = Set.copyOf(roles);
    }
}
