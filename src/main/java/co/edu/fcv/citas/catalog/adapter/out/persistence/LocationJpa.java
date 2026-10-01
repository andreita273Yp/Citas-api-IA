package co.edu.fcv.citas.catalog.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
class LocationJpa {
    @Id private String code;
    private String name;
    private String address;
    protected LocationJpa() { }
    String code() { return code; }
    String name() { return name; }
    String address() { return address; }
}
