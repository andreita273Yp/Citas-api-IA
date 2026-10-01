package co.edu.fcv.citas.scheduling.adapter.in.web;

import co.edu.fcv.citas.scheduling.application.port.in.BookAppointmentUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.BookAppointmentUseCase.Booked;
import co.edu.fcv.citas.scheduling.application.port.in.DecideSpecializedRequestUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.AvailableStart;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.ProfessionalOption;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.SpecialtyOption;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU-021 a HU-024 · Búsqueda de oferta, reserva del USER y decisión ADMIN de solicitudes especializadas. */
@RestController
@RequestMapping("/api/v1")
class BookingController {
    private final SearchAvailabilityUseCase availability;
    private final BookAppointmentUseCase booking;
    private final DecideSpecializedRequestUseCase decisions;

    BookingController(SearchAvailabilityUseCase availability, BookAppointmentUseCase booking, DecideSpecializedRequestUseCase decisions) {
        this.availability = availability;
        this.booking = booking;
        this.decisions = decisions;
    }

    /** {@code type}: GENERAL (Medicina General) o SPECIALIZED; sin filtro devuelve todas las activas. */
    @GetMapping("/catalogs/specialties")
    List<SpecialtyOption> specialties(@RequestParam(required = false) String type) {
        return availability.specialties(type);
    }

    @GetMapping("/catalogs/professionals")
    List<ProfessionalOption> professionals(@RequestParam long specialtyId, @RequestParam long locationId) {
        return availability.professionals(specialtyId, locationId);
    }

    @GetMapping("/availability")
    List<AvailableStart> search(@RequestParam long specialtyId, @RequestParam LocalDate date, @RequestParam(required = false) Long locationId,
                                @RequestParam(required = false) Long professionalId) {
        return availability.search(specialtyId, date, locationId, professionalId);
    }

    /** Solo un USER reserva para sí mismo. */
    @PostMapping("/appointments")
    ResponseEntity<Booked> book(@RequestBody BookingRequest body) {
        if (!CurrentUser.has("USER")) throw new SchedulingException.Forbidden("Solo un usuario paciente puede reservar citas");
        Booked booked = booking.book(CurrentUser.id(), body.command());
        return ResponseEntity.status(HttpStatus.CREATED).body(booked);
    }

    @PostMapping("/admin/appointments/{id}/decision")
    DecideSpecializedRequestUseCase.Decided decide(@PathVariable long id, @RequestBody DecisionRequest body) {
        return decisions.decide(CurrentUser.id(), id, body.decision(), body.reason());
    }

    record BookingRequest(Long professionalId, Long locationId, Long specialtyId, String startAt, String reason) {
        BookAppointmentUseCase.Command command() {
            if (professionalId == null || locationId == null || specialtyId == null || startAt == null)
                throw new SchedulingException.InvalidData("Profesional, sede, especialidad e inicio son obligatorios");
            try {
                return new BookAppointmentUseCase.Command(professionalId, locationId, specialtyId, LocalDateTime.parse(startAt), reason);
            } catch (DateTimeParseException e) {
                throw new SchedulingException.InvalidData("Use fecha y hora YYYY-MM-DDTHH:mm");
            }
        }
    }

    record DecisionRequest(String decision, String reason) { }
}
