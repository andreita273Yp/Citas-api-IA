package co.edu.fcv.citas.support;

import java.sql.Connection;
import java.sql.Statement;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Recrea la base de pruebas desde cero (clean + migrate) al iniciar el contexto y guarda una copia de los
 * catálogos configurables sembrados por Flyway para que {@link TestDatabase#reset()} los restaure.
 */
@Configuration
public class TestDatabaseConfig {
    static final String SPECIALTIES_SNAPSHOT = "test_seed_specialties";

    @Bean
    FlywayMigrationStrategy cleanMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
            try (Connection connection = flyway.getConfiguration().getDataSource().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE " + SPECIALTIES_SNAPSHOT + " AS SELECT * FROM specialties");
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException("No se pudo guardar la copia de catálogos de prueba", e);
            }
        };
    }
}
