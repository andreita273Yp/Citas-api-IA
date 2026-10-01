package co.edu.fcv.citas.support;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Recrea la base de pruebas desde cero (clean + migrate) al iniciar el contexto de pruebas. */
@Configuration
public class TestDatabaseConfig {
    @Bean
    FlywayMigrationStrategy cleanMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
