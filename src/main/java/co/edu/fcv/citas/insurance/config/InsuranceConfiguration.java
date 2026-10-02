package co.edu.fcv.citas.insurance.config;

import co.edu.fcv.citas.insurance.application.AffiliationService;
import co.edu.fcv.citas.insurance.application.InsuranceCatalogService;
import co.edu.fcv.citas.insurance.application.port.in.InsuranceUseCases;
import co.edu.fcv.citas.insurance.application.port.out.InsurancePorts;
import co.edu.fcv.citas.shared.application.TransactionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla los casos de uso de aseguramiento; la aplicación no conoce Spring. */
@Configuration
class InsuranceConfiguration {
    @Bean
    InsuranceUseCases.ManageCatalog manageInsuranceCatalog(InsurancePorts.EpsPort eps, InsurancePorts.PlanPort plans,
                                                           InsurancePorts.RegimePort regimes, TransactionPort tx) {
        return new InsuranceCatalogService(eps, plans, regimes, tx);
    }

    @Bean
    InsuranceUseCases.ManageAffiliation manageAffiliation(InsurancePorts.AffiliationPort affiliations, InsurancePorts.PlanPort plans,
                                                          TransactionPort tx) {
        return new AffiliationService(affiliations, plans, tx);
    }
}
