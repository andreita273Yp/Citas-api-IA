package co.edu.fcv.citas.catalog.application.port.in;

import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;

public interface ReadFixedCatalogUseCase {
    List<CatalogItem> roles();
    List<CatalogItem> appointmentStatuses();
    List<CatalogItem> rescheduleStatuses();
    List<CatalogItem> regimes();
    List<Location> locations();
}
