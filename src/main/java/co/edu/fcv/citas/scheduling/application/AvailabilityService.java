package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.FreeSlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.FreeSlotRow;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.OfferPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SpecialtyRules;
import co.edu.fcv.citas.scheduling.domain.AvailabilityCalculator;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvailabilityService implements SearchAvailabilityUseCase {
    private final OfferPort offer;
    private final FreeSlotPort freeSlots;
    private final Clock clock;

    public AvailabilityService(OfferPort offer, FreeSlotPort freeSlots, Clock clock) {
        this.offer = offer;
        this.freeSlots = freeSlots;
        this.clock = clock;
    }

    @Override
    public List<SpecialtyOption> specialties(String type) {
        if (type == null || type.isBlank()) return offer.activeSpecialties(null);
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "GENERAL" -> offer.activeSpecialties(true);
            case "SPECIALIZED" -> offer.activeSpecialties(false);
            default -> throw new SchedulingException.InvalidData("El tipo debe ser GENERAL o SPECIALIZED");
        };
    }

    @Override
    public List<ProfessionalOption> professionals(long specialtyId, long locationId) {
        return offer.offeredProfessionals(specialtyId, locationId);
    }

    @Override
    public List<AvailableStart> search(long specialtyId, LocalDate date, Long locationId, Long professionalId) {
        if (date == null) throw new SchedulingException.InvalidData("La fecha es obligatoria");
        SpecialtyRules specialty = offer.findActiveSpecialty(specialtyId)
                .orElseThrow(() -> new SchedulingException.NotFound("Especialidad no existe o está inactiva"));
        List<FreeSlotRow> rows = freeSlots.freeSlots(specialtyId, date, locationId, professionalId);
        Map<Long, String> professionalNames = new HashMap<>();
        Map<Long, String> locationCodes = new HashMap<>();
        rows.forEach(r -> {
            professionalNames.put(r.slot().professionalId(), r.professionalName());
            locationCodes.put(r.slot().locationId(), r.locationCode());
        });
        return AvailabilityCalculator.options(rows.stream().map(FreeSlotRow::slot).toList(), specialty.durationMinutes(), LocalDateTime.now(clock))
                .stream()
                .map(o -> new AvailableStart(o.professionalId(), professionalNames.get(o.professionalId()), o.locationId(),
                        locationCodes.get(o.locationId()), o.start(), o.end(), o.durationMinutes()))
                .toList();
    }
}
