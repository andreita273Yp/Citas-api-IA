package co.edu.fcv.citas.identity.config;

import co.edu.fcv.citas.identity.application.RegistrationService;
import co.edu.fcv.citas.identity.application.SessionService;
import co.edu.fcv.citas.identity.application.StaffAccountService;
import co.edu.fcv.citas.identity.application.port.in.CreateStaffAccountUseCase;
import co.edu.fcv.citas.identity.application.port.in.RegisterUserUseCase;
import co.edu.fcv.citas.identity.application.port.in.SessionUseCase;
import co.edu.fcv.citas.identity.application.port.out.InsuranceAffiliationPort;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.RefreshSessionPort;
import co.edu.fcv.citas.identity.application.port.out.TokenPort;
import co.edu.fcv.citas.shared.application.TransactionPort;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla los casos de uso de identidad con sus adaptadores; la aplicación no conoce Spring. */
@Configuration
class IdentityConfiguration {
    @Bean
    RegisterUserUseCase registerUserUseCase(UserAccountPort users, PasswordHasher passwords, InsuranceAffiliationPort affiliations,
                                            TransactionPort tx) {
        return new RegistrationService(users, passwords, affiliations, tx);
    }

    @Bean
    SessionUseCase sessionUseCase(UserAccountPort users, RefreshSessionPort sessions, PasswordHasher passwords, TokenPort tokens,
                                  TransactionPort tx) {
        return new SessionService(users, sessions, passwords, tokens, tx);
    }

    @Bean
    CreateStaffAccountUseCase createStaffAccountUseCase(UserAccountPort users, PasswordHasher passwords, TransactionPort tx) {
        return new StaffAccountService(users, passwords, tx);
    }
}
