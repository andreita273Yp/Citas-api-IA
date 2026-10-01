package co.edu.fcv.citas.catalog.config;

import co.edu.fcv.citas.catalog.application.FixedCatalogQueryService;
import co.edu.fcv.citas.catalog.application.port.in.ReadFixedCatalogUseCase;
import co.edu.fcv.citas.catalog.application.port.out.FixedCatalogReadPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla el caso de uso de catálogos fijos; la aplicación no conoce Spring. */
@Configuration
class CatalogConfiguration {
    @Bean
    ReadFixedCatalogUseCase readFixedCatalogUseCase(FixedCatalogReadPort port) {
        return new FixedCatalogQueryService(port);
    }
}
