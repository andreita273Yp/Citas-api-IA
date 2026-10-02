package co.edu.fcv.citas.scheduling.adapter.in.web;

import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.AppointmentDetail;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.MyAppointments;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.Reschedule;
import co.edu.fcv.citas.scheduling.application.port.in.AppointmentLifecycleUseCases.RescheduleView;
import co.edu.fcv.citas.scheduling.domain.AppointmentStatus;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU-025 a HU-028 · Mis citas, cancelación, reprogramación y su decisión ADMIN (`/admin/**` exige ADMIN). */
@RestController
@RequestMapping("/api/v1")
class MyAppointmentsController {
    private final MyAppointments appointments;
    private final Reschedule reschedules;

    MyAppointmentsController(MyAppointments appointments, Reschedule reschedules) {
        this.appointments = appointments;
        this.reschedules = reschedules;
    }

    @GetMapping("/appointments")
    List<AppointmentDetail> list(@RequestParam(required = false) String status, @RequestParam(required = false) LocalDate from,
                                 @RequestParam(required = false) LocalDate to) {
        return appointments.list(CurrentUser.id(), status(status), from, to);
    }

    @GetMapping("/appointments/{id}")
    AppointmentDetail detail(@PathVariable long id) {
        return appointments.detail(CurrentUser.id(), id);
    }

    @PostMapping("/appointments/{id}/cancel")
    AppointmentDetail cancel(@PathVariable long id) {
        return appointments.cancel(CurrentUser.id(), id);
    }

    @PostMapping("/appointments/{id}/reschedule-requests")
    ResponseEntity<RescheduleView> requestReschedule(@PathVariable long id, @RequestBody RescheduleRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reschedules.request(CurrentUser.id(), id, body.start(), body.locationId()));
    }

    @PostMapping("/appointments/{id}/reschedule-requests/{requestId}/keep")
    ResponseEntity<Void> keep(@PathVariable long id, @PathVariable long requestId) {
        appointments.keepAfterRejection(CurrentUser.id(), id, requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/reschedule-requests/{id}/decision")
    RescheduleView decide(@PathVariable long id, @RequestBody DecisionRequest body) {
        return reschedules.decide(CurrentUser.id(), id, body.decision(), body.reason());
    }

    private static AppointmentStatus status(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return AppointmentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SchedulingException.InvalidData("Estado de cita desconocido");
        }
    }

    record RescheduleRequest(String startAt, Long locationId) {
        LocalDateTime start() {
            if (startAt == null) throw new SchedulingException.InvalidData("La nueva fecha y hora son obligatorias");
            try {
                return LocalDateTime.parse(startAt);
            } catch (DateTimeParseException e) {
                throw new SchedulingException.InvalidData("Use fecha y hora YYYY-MM-DDTHH:mm");
            }
        }
    }

    record DecisionRequest(String decision, String reason) { }
}
