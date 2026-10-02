package co.edu.fcv.citas.shared.config;

import co.edu.fcv.citas.shared.application.TransactionPort;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

/** Implementa el puerto de transacciones con {@link TransactionTemplate} (propagación REQUIRED). */
@Configuration
class TransactionConfiguration {
    @Bean
    TransactionPort transactionPort(TransactionTemplate template) {
        return new TransactionPort() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }
}
