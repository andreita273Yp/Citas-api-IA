package co.edu.fcv.citas.catalog.application;

import co.edu.fcv.citas.catalog.application.port.in.ReadFixedCatalogUseCase;
import co.edu.fcv.citas.catalog.application.port.out.FixedCatalogReadPort;
import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FixedCatalogQueryService implements ReadFixedCatalogUseCase {
    private final FixedCatalogReadPort catalogReadPort;

    public FixedCatalogQueryService(FixedCatalogReadPort catalogReadPort) {
        this.catalogReadPort = catalogReadPort;
    }

    @Override
    public List<CatalogItem> roles() {
        return catalogReadPort.findByType("ROLE");
    }

    @Override
    public List<CatalogItem> appointmentStatuses() {
        return catalogReadPort.findByType("APPOINTMENT_STATUS");
    }

    @Override
    public List<CatalogItem> rescheduleStatuses() {
        return catalogReadPort.findByType("RESCHEDULE_STATUS");
    }

    @Override
    public List<CatalogItem> regimes() {
        return catalogReadPort.findByType("REGIME");
    }

    @Override
    public List<Location> locations() {
        return catalogReadPort.findLocations();
    }
}
