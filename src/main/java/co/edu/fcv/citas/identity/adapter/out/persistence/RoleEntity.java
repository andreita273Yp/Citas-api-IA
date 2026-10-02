package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catálogo fijo `roles` (solo lectura). */
@Entity
@Table(name = "roles")
class RoleEntity {
    @Id
    private Integer id;
    @Column(nullable = false, unique = true)
    private String code;

    protected RoleEntity() { }

    String code() { return code; }
}
