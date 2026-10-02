package co.edu.fcv.citas.identity.adapter.in.web;

import co.edu.fcv.citas.identity.domain.UserAccount;
import java.util.Set;
import java.util.stream.Collectors;

/** Contrato JSON de `/api/v1/auth`; la validación de negocio vive en el dominio. */
public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(String firstName, String lastName, String documentType, String documentNumber, String email,
                                  String phone, String password, Long insurancePlanId) { }

    public record LoginRequest(String email, String password) { }

    public record UserResponse(String id, String firstName, String lastName, String documentType, String documentNumber,
                               String email, String phone, Set<String> roles) {
        static UserResponse of(UserAccount a) {
            var p = a.personal();
            return new UserResponse(Long.toString(a.id()), p.firstName(), p.lastName(), p.document().type(), p.document().number(),
                    p.email().value(), p.phone(), a.roles().stream().map(Enum::name).collect(Collectors.toSet()));
        }
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }
}
