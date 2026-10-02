package co.edu.fcv.citas.scheduling.adapter.in.web;

import co.edu.fcv.citas.offer.application.port.in.ManageProfessionalsUseCase;
import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase.BlockView;
import co.edu.fcv.citas.scheduling.domain.BlockSchedule;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HU-018/019/020 · `/api/v1/professional` (solo PROFESSIONAL). Fechas `YYYY-MM-DD`, horas `HH:mm`. */
@RestController
@RequestMapping("/api/v1/professional")
class ProfessionalAgendaController {
    private final ManageBlocksUseCase agenda;
    private final ManageProfessionalsUseCase professionals;

    ProfessionalAgendaController(ManageBlocksUseCase agenda, ManageProfessionalsUseCase professionals) {
        this.agenda = agenda;
        this.professionals = professionals;
    }

    /** Perfil propio con sus sedes y especialidades vigentes, para publicar bloques. */
    @GetMapping("/me")
    ProfessionalView me() {
        return professionals.get(agenda.professionalIdOf(CurrentUser.id()));
    }

    @GetMapping("/blocks")
    List<BlockResponse> list(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) Long locationId) {
        return agenda.list(CurrentUser.id(), from, to, locationId).stream().map(BlockResponse::of).toList();
    }

    @PostMapping("/blocks")
    ResponseEntity<BlockResponse> create(@RequestBody BlockRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BlockResponse.of(agenda.create(CurrentUser.id(), body.location(), body.schedule())));
    }

    @PutMapping("/blocks/{id}")
    BlockResponse update(@PathVariable long id, @RequestBody BlockRequest body) {
        return BlockResponse.of(agenda.update(CurrentUser.id(), id, body.location(), body.schedule()));
    }

    @DeleteMapping("/blocks/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        agenda.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    record BlockRequest(Long locationId, String date, String startTime, String endTime) {
        long location() {
            if (locationId == null) throw new SchedulingException.InvalidData("La sede es obligatoria");
            return locationId;
        }

        BlockSchedule schedule() {
            try {
                return new BlockSchedule(date == null ? null : LocalDate.parse(date), startTime == null ? null : LocalTime.parse(startTime),
                        endTime == null ? null : LocalTime.parse(endTime));
            } catch (DateTimeParseException e) {
                throw new SchedulingException.InvalidData("Use fecha YYYY-MM-DD y horas HH:mm");
            }
        }
    }

    record BlockResponse(long id, long locationId, String locationCode, String date, String startTime, String endTime, int slots, int bookedSlots) {
        static BlockResponse of(BlockView b) {
            return new BlockResponse(b.id(), b.locationId(), b.locationCode(), b.schedule().date().toString(), b.schedule().start().toString(),
                    b.schedule().end().toString(), b.slots(), b.bookedSlots());
        }
    }
}
