package co.edu.fcv.citas.identity.adapter.in.web;

import co.edu.fcv.citas.identity.adapter.in.web.AuthDtos.LoginRequest;
import co.edu.fcv.citas.identity.adapter.in.web.AuthDtos.RegisterRequest;
import co.edu.fcv.citas.identity.adapter.in.web.AuthDtos.TokenResponse;
import co.edu.fcv.citas.identity.adapter.in.web.AuthDtos.UserResponse;
import co.edu.fcv.citas.identity.application.port.in.RegisterUserUseCase;
import co.edu.fcv.citas.identity.application.port.in.SessionUseCase;
import co.edu.fcv.citas.identity.domain.IdentityException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador REST de identidad: access en JSON, refresh solo en cookie HttpOnly. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    static final String REFRESH_COOKIE = "refresh_token";

    private final RegisterUserUseCase registration;
    private final SessionUseCase sessions;
    private final boolean secureCookie;

    public AuthController(RegisterUserUseCase registration, SessionUseCase sessions, @Value("${app.cookie.secure}") boolean secureCookie) {
        this.registration = registration;
        this.sessions = sessions;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@RequestBody RegisterRequest r) {
        var account = registration.register(new RegisterUserUseCase.Command(r.firstName(), r.lastName(), r.documentType(),
                r.documentNumber(), r.email(), r.phone(), r.password(), r.insurancePlanId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(account));
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@RequestBody LoginRequest r) {
        return withSession(sessions.login(r.email(), r.password()));
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String refresh) {
        if (refresh == null || refresh.isBlank()) throw new IdentityException.InvalidSession();
        return withSession(sessions.refresh(refresh));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refresh) {
        if (refresh != null && !refresh.isBlank()) sessions.logout(refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseEntity<TokenResponse> withSession(SessionUseCase.IssuedSession s) {
        Duration maxAge = Duration.between(Instant.now(), s.refreshExpiresAt());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(s.refreshToken(), maxAge).toString())
                .body(new TokenResponse(s.accessToken(), "Bearer", s.accessExpiresInSeconds()));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(secureCookie).sameSite(secureCookie ? "None" : "Lax")
                .path("/api/v1/auth").maxAge(maxAge).build();
    }
}
