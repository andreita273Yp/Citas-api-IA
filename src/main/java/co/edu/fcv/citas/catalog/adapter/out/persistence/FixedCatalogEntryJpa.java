package co.edu.fcv.citas.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fixed_catalog_entries")
class FixedCatalogEntryJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "catalog_type") private String catalogType;
    private String code;
    @Column(name = "display_name") private String displayName;
    protected FixedCatalogEntryJpa() { }
    String code() { return code; }
    String displayName() { return displayName; }
}
