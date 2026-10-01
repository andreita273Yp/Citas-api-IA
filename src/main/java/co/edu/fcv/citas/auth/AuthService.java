package co.edu.fcv.citas.auth;

import co.edu.fcv.citas.auth.AuthDtos.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authentication adapter for the professor's users, roles and refresh_tokens tables. */
@Service
public class AuthService {
 private final JdbcTemplate db; private final PasswordEncoder passwords; private final JwtService jwt;
 public AuthService(JdbcTemplate db,PasswordEncoder passwords,JwtService jwt){this.db=db;this.passwords=passwords;this.jwt=jwt;}
 @Transactional public UserResponse register(RegisterRequest r){
  String email=normal(r.email()),type=normal(r.documentType()),document=normal(r.documentNumber());
  if(r.password().getBytes(StandardCharsets.UTF_8).length>72)throw new ApiException(HttpStatus.BAD_REQUEST,"La contraseña supera el límite permitido");
  if(db.queryForObject("select count(*) from users where email=? or (document_type=? and document_number=?)",Integer.class,email,type,document)>0)throw new ApiException(HttpStatus.CONFLICT,"Email o documento ya registrado");
  db.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values(?,?,?,?,?,?,?,true,false)",r.firstName().trim(),r.lastName().trim(),type,document,email,r.phone().trim(),passwords.encode(r.password()));
  long id=db.queryForObject("select id from users where email=?",Long.class,email); db.update("insert into user_roles(user_id,role_id) values(?,(select id from roles where code='USER'))",id);
  if(r.insurancePlanId()!=null){if(db.queryForObject("select count(*) from eps_plans where id=? and active=true",Integer.class,r.insurancePlanId())==0)throw new ApiException(HttpStatus.BAD_REQUEST,"Plan de afiliación inexistente o inactivo");db.update("insert into user_insurance_affiliations(user_id,plan_id,membership_number,is_current,valid_from) values(?,?,?,true,curdate())",id,r.insurancePlanId(),"AF-"+id+"-"+UUID.randomUUID().toString().substring(0,8));}
  return response(principalById(id));
 }
 @Transactional public Issued login(LoginRequest r){Principal p=byEmail(normal(r.email()));if(!passwords.matches(r.password(),p.passwordHash))throw new ApiException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");return issue(p);}
 @Transactional public Issued refresh(String token){try{var c=jwt.refreshClaims(token);if(!"refresh".equals(c.get("typ",String.class)))throw new IllegalArgumentException();List<Long> ids=db.query("select id from refresh_tokens where token_hash=? and revoked_at is null and expires_at>now() for update",(rs,n)->rs.getLong(1),hash(c.getId()));if(ids.isEmpty())throw new ApiException(HttpStatus.UNAUTHORIZED,"Refresh inválido");db.update("update refresh_tokens set revoked_at=now() where id=?",ids.getFirst());return issue(principalById(Long.parseLong(c.getSubject())));}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(HttpStatus.UNAUTHORIZED,"Refresh inválido");}}
 @Transactional public void logout(String token){try{db.update("update refresh_tokens set revoked_at=now() where token_hash=? and revoked_at is null",hash(jwt.refreshClaims(token).getId()));}catch(Exception ignored){}}
 private Issued issue(Principal p){String jti=UUID.randomUUID().toString();db.update("insert into refresh_tokens(user_id,token_hash,expires_at,created_at) values(?,?,?,now())",p.id,hash(jti),LocalDateTime.now().plusDays(7));return new Issued(new TokenResponse(jwt.access(p.jwt()),"Bearer",jwt.accessSeconds()),jwt.refresh(p.jwt(),jti));}
 private Principal byEmail(String email){return one("select id,first_name,last_name,document_type,document_number,email,phone,password_hash from users where email=? and active=true",email);}
 private Principal principalById(long id){return one("select id,first_name,last_name,document_type,document_number,email,phone,password_hash from users where id=? and active=true",id);}
 private Principal one(String sql,Object value){List<Principal> found=db.query(sql,(rs,n)->principal(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)),value);if(found.isEmpty())throw new ApiException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas");return found.getFirst();}
 private Principal principal(long id,String first,String last,String type,String document,String email,String phone,String passwordHash){List<String> roles=db.query("select r.code from roles r join user_roles ur on ur.role_id=r.id where ur.user_id=?",(rs,n)->rs.getString(1),id);return new Principal(id,first,last,type,document,email,phone,passwordHash,roles);}
 private UserResponse response(Principal p){return new UserResponse(Long.toString(p.id),p.first,p.last,p.type,p.document,p.email,p.phone,Set.copyOf(p.roles));}
 private String normal(String value){return value.trim().toLowerCase(Locale.ROOT);}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public record Issued(TokenResponse tokens,String refreshToken){}
 private record Principal(long id,String first,String last,String type,String document,String email,String phone,String passwordHash,List<String> roles){JwtService.Principal jwt(){return new JwtService.Principal(Long.toString(id),roles);}}
}
