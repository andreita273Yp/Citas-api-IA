package co.edu.fcv.citas.identity;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="refresh_sessions") public class RefreshSession {
 @Id private String id=UUID.randomUUID().toString(); @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user; @Column(name="jti_hash",length=64) private String jtiHash; @Column(name="expires_at") private Instant expiresAt; @Column(name="revoked_at") private Instant revokedAt; @Column(name="created_at") private Instant createdAt=Instant.now();
 protected RefreshSession(){} public RefreshSession(User user,String jtiHash,Instant expiresAt){this.user=user;this.jtiHash=jtiHash;this.expiresAt=expiresAt;} public User user(){return user;} public boolean active(Instant now){return revokedAt==null&&expiresAt.isAfter(now);} public void revoke(){revokedAt=Instant.now();}
}
