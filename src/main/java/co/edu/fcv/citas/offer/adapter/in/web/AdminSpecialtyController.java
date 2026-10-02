package co.edu.fcv.citas.offer.adapter.in.web;

import co.edu.fcv.citas.offer.application.port.in.ManageSpecialtiesUseCase;
import co.edu.fcv.citas.offer.domain.Specialty;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-014 · `/api/v1/admin/specialties` (solo ADMIN, ver SecurityConfig). No existe DELETE: se desactiva. */
@RestController
@RequestMapping("/api/v1/admin/specialties")
class AdminSpecialtyController {
    private final ManageSpecialtiesUseCase specialties;

    AdminSpecialtyController(ManageSpecialtiesUseCase specialties) {
        this.specialties = specialties;
    }

    @GetMapping
    List<SpecialtyResponse> list() {
        return specialties.list().stream().map(SpecialtyResponse::of).toList();
    }

    @PostMapping
    ResponseEntity<SpecialtyResponse> create(@RequestBody CreateSpecialty body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecialtyResponse.of(specialties.create(body.code(), body.name(), body.durationMinutes())));
    }

    @PatchMapping("/{id}")
    SpecialtyResponse update(@PathVariable long id, @RequestBody UpdateSpecialty body) {
        return SpecialtyResponse.of(specialties.update(id, body.name(), body.durationMinutes(), body.active()));
    }

    record CreateSpecialty(String code, String name, Integer durationMinutes) { }

    record UpdateSpecialty(String name, Integer durationMinutes, Boolean active) { }

    record SpecialtyResponse(long id, String code, String name, int durationMinutes, boolean general, boolean requiresAdminApproval, boolean active) {
        static SpecialtyResponse of(Specialty s) {
            return new SpecialtyResponse(s.id(), s.code(), s.name(), s.duration().minutes(), s.general(), s.requiresAdminApproval(), s.active());
        }
    }
}
