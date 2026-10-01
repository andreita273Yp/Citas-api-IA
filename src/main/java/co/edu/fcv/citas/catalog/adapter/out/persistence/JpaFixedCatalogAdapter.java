package co.edu.fcv.citas.catalog.adapter.out.persistence;

import co.edu.fcv.citas.catalog.application.port.out.FixedCatalogReadPort;
import co.edu.fcv.citas.catalog.domain.CatalogItem;
import co.edu.fcv.citas.catalog.domain.Location;
import java.util.List;

/**
 * Obsoleto: leía el modelo provisional de V2 (fixed_catalog_entries). Ya no es un bean;
 * lo reemplaza {@link JdbcFixedCatalogAdapter} sobre el modelo de referencia V6.
 */
@Deprecated
public class JpaFixedCatalogAdapter implements FixedCatalogReadPort {
    private final FixedCatalogEntryRepository entries;
    private final LocationRepository locations;

    public JpaFixedCatalogAdapter(FixedCatalogEntryRepository entries, LocationRepository locations) {
        this.entries = entries;
        this.locations = locations;
    }

    @Override
    public List<CatalogItem> findByType(String type) {
        return entries.findByCatalogTypeOrderByDisplayNameAsc(type).stream()
                .map(entry -> new CatalogItem(entry.code(), entry.displayName()))
                .toList();
    }

    @Override
    public List<Location> findLocations() {
        return locations.findAllByOrderByNameAsc().stream()
                .map(location -> new Location(location.code(), location.code(), location.name(), location.address(), null, null))
                .toList();
    }
}
