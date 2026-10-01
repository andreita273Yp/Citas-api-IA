package co.edu.fcv.citas.auth;
import co.edu.fcv.citas.auth.AuthDtos.*; import jakarta.validation.Valid; import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.time.Duration;
@RestController @RequestMapping("/api/v1/auth") public class AuthController {
 private final AuthService auth; private final boolean secure; public AuthController(AuthService auth,@Value("${app.cookie.secure}") boolean secure){this.auth=auth;this.secure=secure;}
 @PostMapping("/register") ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(r));}
 @PostMapping("/login") ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest r){var issued=auth.login(r);return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(issued.refreshToken(),Duration.ofDays(7)).toString()).body(issued.tokens());}
 @PostMapping("/refresh") ResponseEntity<TokenResponse> refresh(@CookieValue(value="refresh_token",required=false) String refresh){if(refresh==null)throw new ApiException(HttpStatus.UNAUTHORIZED,"Refresh inválido");var issued=auth.refresh(refresh);return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(issued.refreshToken(),Duration.ofDays(7)).toString()).body(issued.tokens());}
 @PostMapping("/logout") ResponseEntity<Void> logout(@CookieValue(value="refresh_token",required=false) String refresh){if(refresh!=null)auth.logout(refresh);return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie("",Duration.ZERO).toString()).build();}
 private ResponseCookie cookie(String value,Duration age){return ResponseCookie.from("refresh_token",value).httpOnly(true).secure(secure).sameSite(secure?"None":"Lax").path("/api/v1/auth").maxAge(age).build();}
}
