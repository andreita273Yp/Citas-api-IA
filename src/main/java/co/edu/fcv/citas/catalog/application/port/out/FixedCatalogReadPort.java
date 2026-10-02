package co.edu.fcv.citas.catalog.application.port.out;

import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;

public interface FixedCatalogReadPort {
    List<CatalogItem> findByType(String type);
    List<Location> findLocations();
}
