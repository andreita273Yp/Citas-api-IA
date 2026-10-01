package co.edu.fcv.citas.catalog.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FixedCatalogEntryRepository extends JpaRepository<FixedCatalogEntryJpa, Long> {
    List<FixedCatalogEntryJpa> findByCatalogTypeOrderByDisplayNameAsc(String catalogType);
}
