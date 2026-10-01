package co.edu.fcv.citas.auth;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.time.*; import java.util.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
@Service public class JwtService {
 private final SecretKey accessKey,refreshKey; private final Duration accessTtl,refreshTtl;
 public JwtService(@Value("${app.jwt.access-secret}") String access,@Value("${app.jwt.refresh-secret}") String refresh,@Value("${app.jwt.access-ttl}") Duration accessTtl,@Value("${app.jwt.refresh-ttl}") Duration refreshTtl){this.accessKey=key(access);this.refreshKey=key(refresh);this.accessTtl=accessTtl;this.refreshTtl=refreshTtl;}
 private SecretKey key(String value){if(value.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT secret must be at least 32 bytes");return Keys.hmacShaKeyFor(value.getBytes(StandardCharsets.UTF_8));}
 public String access(Principal u){return Jwts.builder().subject(u.id()).claim("roles",u.roles()).claim("typ","access").issuedAt(new Date()).expiration(Date.from(Instant.now().plus(accessTtl))).signWith(accessKey).compact();}
 public String refresh(Principal u,String jti){return Jwts.builder().subject(u.id()).id(jti).claim("typ","refresh").issuedAt(new Date()).expiration(Date.from(Instant.now().plus(refreshTtl))).signWith(refreshKey).compact();}
 public Claims accessClaims(String token){return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();}
 public Claims refreshClaims(String token){return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();}
 public long accessSeconds(){return accessTtl.toSeconds();} public Instant refreshExpiry(){return Instant.now().plus(refreshTtl);}
 public record Principal(String id,List<String> roles){}
}
