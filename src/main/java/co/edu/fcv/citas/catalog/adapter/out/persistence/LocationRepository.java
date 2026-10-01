package co.edu.fcv.citas.catalog.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface LocationRepository extends JpaRepository<LocationJpa, String> {
    List<LocationJpa> findAllByOrderByNameAsc();
}
