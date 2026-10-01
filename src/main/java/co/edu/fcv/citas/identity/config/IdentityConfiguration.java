package co.edu.fcv.citas.identity.config;

import co.edu.fcv.citas.identity.application.RegistrationService;
import co.edu.fcv.citas.identity.application.SessionService;
import co.edu.fcv.citas.identity.application.port.in.RegisterUserUseCase;
import co.edu.fcv.citas.identity.application.port.in.SessionUseCase;
import co.edu.fcv.citas.identity.application.port.out.InsuranceAffiliationPort;
import co.edu.fcv.citas.identity.application.port.out.PasswordHasher;
import co.edu.fcv.citas.identity.application.port.out.RefreshSessionPort;
import co.edu.fcv.citas.identity.application.port.out.TokenPort;
import co.edu.fcv.citas.identity.application.port.out.TransactionPort;
import co.edu.fcv.citas.identity.application.port.out.UserAccountPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Ensambla los casos de uso de identidad con sus adaptadores; la aplicación no conoce Spring. */
@Configuration
class IdentityConfiguration {
    @Bean
    TransactionPort identityTransactions(TransactionTemplate template) {
        return new TransactionPort() {
            @Override
            public <T> T inTransaction(java.util.function.Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserAccountPort users, PasswordHasher passwords, InsuranceAffiliationPort affiliations,
                                            TransactionPort identityTransactions) {
        return new RegistrationService(users, passwords, affiliations, identityTransactions);
    }

    @Bean
    SessionUseCase sessionUseCase(UserAccountPort users, RefreshSessionPort sessions, PasswordHasher passwords, TokenPort tokens,
                                  TransactionPort identityTransactions) {
        return new SessionService(users, sessions, passwords, tokens, identityTransactions);
    }
}
