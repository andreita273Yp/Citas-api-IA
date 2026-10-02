package co.edu.fcv.citas.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** Tabla `users` del modelo de referencia; created_at/updated_at los asigna la base. */
@Entity
@Table(name = "users")
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false) private String firstName;
    @Column(name = "last_name", nullable = false) private String lastName;
    @Column(name = "document_type", nullable = false) private String documentType;
    @Column(name = "document_number", nullable = false) private String documentNumber;
    @Column(nullable = false) private String email;
    private String phone;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    protected UserEntity() { }

    UserEntity(String firstName, String lastName, String documentType, String documentNumber, String email, String phone,
               String passwordHash, RoleEntity role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.roles.add(role);
    }

    Long id() { return id; }
    String firstName() { return firstName; }
    String lastName() { return lastName; }
    String documentType() { return documentType; }
    String documentNumber() { return documentNumber; }
    String email() { return email; }
    String phone() { return phone; }
    String passwordHash() { return passwordHash; }
    boolean active() { return active; }
    Set<RoleEntity> roles() { return roles; }

    void changePasswordHash(String hash) { this.passwordHash = hash; }

    void changePhone(String value) { this.phone = value; }
}
