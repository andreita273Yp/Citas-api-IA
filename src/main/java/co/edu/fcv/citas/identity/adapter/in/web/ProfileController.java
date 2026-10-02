package co.edu.fcv.citas.identity.adapter.in.web;

import co.edu.fcv.citas.identity.adapter.in.web.AuthDtos.UserResponse;
import co.edu.fcv.citas.identity.application.port.in.PasswordRecoveryUseCase;
import co.edu.fcv.citas.identity.application.port.in.ProfileUseCase;
import co.edu.fcv.citas.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-008/009 recuperación de contraseña · HU-010 perfil propio (solo teléfono editable). */
@RestController
@RequestMapping("/api/v1")
class ProfileController {
    private final ProfileUseCase profiles;
    private final PasswordRecoveryUseCase recovery;

    ProfileController(ProfileUseCase profiles, PasswordRecoveryUseCase recovery) {
        this.profiles = profiles;
        this.recovery = recovery;
    }

    @GetMapping("/users/me")
    UserResponse me() {
        return UserResponse.of(profiles.me(CurrentUser.id()));
    }

    /** Solo se lee {@code phone}; cualquier otro campo del cuerpo se ignora. */
    @PatchMapping("/users/me")
    UserResponse updatePhone(@RequestBody PhoneChange body) {
        return UserResponse.of(profiles.updatePhone(CurrentUser.id(), body.phone()));
    }

    /** Respuesta 202 idéntica exista o no la cuenta (HU-008 CA-02). */
    @PostMapping("/auth/password-recovery")
    ResponseEntity<Void> requestRecovery(@RequestBody RecoveryRequest body) {
        recovery.request(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/password-reset")
    ResponseEntity<Void> reset(@RequestBody ResetRequest body) {
        recovery.reset(body.token(), body.password(), body.confirmation());
        return ResponseEntity.noContent().build();
    }

    record PhoneChange(String phone) { }

    record RecoveryRequest(String email) { }

    record ResetRequest(String token, String password, String confirmation) { }
}
