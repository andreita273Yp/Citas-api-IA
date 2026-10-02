package co.edu.fcv.citas.catalog.adapter.in.web;

import co.edu.fcv.citas.catalog.application.port.in.ReadFixedCatalogUseCase;
import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogs")
public class CatalogController {
    private final ReadFixedCatalogUseCase catalogs;

    public CatalogController(ReadFixedCatalogUseCase catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/roles")
    List<CatalogItem> roles() { return catalogs.roles(); }

    @GetMapping("/appointment-statuses")
    List<CatalogItem> appointmentStatuses() { return catalogs.appointmentStatuses(); }

    @GetMapping("/reschedule-statuses")
    List<CatalogItem> rescheduleStatuses() { return catalogs.rescheduleStatuses(); }

    @GetMapping("/regimes")
    List<CatalogItem> regimes() { return catalogs.regimes(); }

    @GetMapping("/locations")
    List<Location> locations() { return catalogs.locations(); }
}
