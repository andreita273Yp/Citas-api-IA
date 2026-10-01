package co.edu.fcv.citas.identity;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="users") public class User {
 @Id private String id=UUID.randomUUID().toString(); @Column(name="first_name") private String firstName; @Column(name="last_name") private String lastName; @Column(name="document_type") private String documentType; @Column(name="document_number") private String documentNumber; private String email; private String phone; @Column(name="password_hash") private String passwordHash; @Column(name="created_at") private Instant createdAt=Instant.now();
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="user_roles",joinColumns=@JoinColumn(name="user_id")) @Column(name="role_name") @Enumerated(EnumType.STRING) private Set<Role> roles=new HashSet<>();
 protected User(){} public User(String firstName,String lastName,String documentType,String documentNumber,String email,String phone,String passwordHash){this.firstName=firstName;this.lastName=lastName;this.documentType=documentType;this.documentNumber=documentNumber;this.email=email;this.phone=phone;this.passwordHash=passwordHash;roles.add(Role.USER);}
 public String id(){return id;} public String email(){return email;} public String passwordHash(){return passwordHash;} public Set<Role> roles(){return Set.copyOf(roles);} public String firstName(){return firstName;} public String lastName(){return lastName;} public String documentType(){return documentType;} public String documentNumber(){return documentNumber;} public String phone(){return phone;}
 public void changePhone(String value){this.phone=value;} public void changePasswordHash(String value){this.passwordHash=value;} public void grant(Role role){roles.add(role);}
}
