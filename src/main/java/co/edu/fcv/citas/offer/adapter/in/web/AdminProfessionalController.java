package co.edu.fcv.citas.offer.adapter.in.web;

import co.edu.fcv.citas.offer.application.port.in.ManageProfessionalsUseCase;
import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.SpecialtyAssignments;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-015/016/017 · `/api/v1/admin/professionals` (solo ADMIN). La contraseña inicial nunca se devuelve. */
@RestController
@RequestMapping("/api/v1/admin/professionals")
class AdminProfessionalController {
    private final ManageProfessionalsUseCase professionals;

    AdminProfessionalController(ManageProfessionalsUseCase professionals) {
        this.professionals = professionals;
    }

    @GetMapping
    List<ProfessionalResponse> list() {
        return professionals.list().stream().map(ProfessionalResponse::of).toList();
    }

    @GetMapping("/{id}")
    ProfessionalResponse get(@PathVariable long id) {
        return ProfessionalResponse.of(professionals.get(id));
    }

    @PostMapping
    ResponseEntity<ProfessionalResponse> create(@RequestBody ManageProfessionalsUseCase.NewProfessional body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfessionalResponse.of(professionals.create(body)));
    }

    @PatchMapping("/{id}")
    ProfessionalResponse setActive(@PathVariable long id, @RequestBody StatusChange body) {
        if (body.active() == null) throw new OfferException.InvalidData("Indique el estado activo");
        return ProfessionalResponse.of(professionals.setActive(id, body.active()));
    }

    @PutMapping("/{id}/specialties")
    ProfessionalResponse assignSpecialties(@PathVariable long id, @RequestBody SpecialtiesChange body) {
        return ProfessionalResponse.of(professionals.assignSpecialties(id, new SpecialtyAssignments(body.assignments())));
    }

    @PutMapping("/{id}/locations")
    ProfessionalResponse assignLocations(@PathVariable long id, @RequestBody LocationsChange body) {
        return ProfessionalResponse.of(professionals.assignLocations(id, body.locationIds()));
    }

    record ProfessionalResponse(long id, long userId, String firstName, String lastName, String email, String phone,
                                String professionalCode, String licenseNumber, boolean active,
                                List<ProfessionalView.AssignedSpecialty> specialties, List<ProfessionalView.AssignedLocation> locations) {
        static ProfessionalResponse of(ProfessionalView v) {
            return new ProfessionalResponse(v.id(), v.userId(), v.firstName(), v.lastName(), v.email(), v.phone(),
                    v.credentials().professionalCode(), v.credentials().licenseNumber(), v.active(), v.specialties(), v.locations());
        }
    }

    record StatusChange(Boolean active) { }

    record SpecialtiesChange(List<SpecialtyAssignments.Assignment> assignments) { }

    record LocationsChange(List<Long> locationIds) { }
}
