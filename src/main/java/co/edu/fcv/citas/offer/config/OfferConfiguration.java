package co.edu.fcv.citas.offer.config;

import co.edu.fcv.citas.offer.application.ProfessionalService;
import co.edu.fcv.citas.offer.application.SpecialtyService;
import co.edu.fcv.citas.offer.application.port.in.ManageProfessionalsUseCase;
import co.edu.fcv.citas.offer.application.port.in.ManageSpecialtiesUseCase;
import co.edu.fcv.citas.offer.application.port.out.LocationCatalogPort;
import co.edu.fcv.citas.offer.application.port.out.ProfessionalAccountPort;
import co.edu.fcv.citas.offer.application.port.out.ProfessionalRepositoryPort;
import co.edu.fcv.citas.offer.application.port.out.SpecialtyCatalogPort;
import co.edu.fcv.citas.shared.application.TransactionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla los casos de uso de la oferta de atención; la aplicación no conoce Spring. */
@Configuration
class OfferConfiguration {
    @Bean
    ManageSpecialtiesUseCase manageSpecialtiesUseCase(SpecialtyCatalogPort specialties, TransactionPort tx) {
        return new SpecialtyService(specialties, tx);
    }

    @Bean
    ManageProfessionalsUseCase manageProfessionalsUseCase(ProfessionalRepositoryPort professionals, SpecialtyCatalogPort specialties,
                                                          LocationCatalogPort locations, ProfessionalAccountPort accounts, TransactionPort tx) {
        return new ProfessionalService(professionals, specialties, locations, accounts, tx);
    }
}
