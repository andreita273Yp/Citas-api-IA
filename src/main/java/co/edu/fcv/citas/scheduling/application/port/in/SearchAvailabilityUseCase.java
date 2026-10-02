package co.edu.fcv.citas.scheduling.application.port.in;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-021 · Consulta de la oferta reservable con los filtros del PRD (RF-10). */
public interface SearchAvailabilityUseCase {
    /** {@code type} es GENERAL, SPECIALIZED o nulo (todas). Solo especialidades activas. */
    List<SpecialtyOption> specialties(String type);

    /** Profesionales activos con la especialidad activa asociada y la sede asignada. */
    List<ProfessionalOption> professionals(long specialtyId, long locationId);

    /** Inicios reservables completos; {@code locationId} y {@code professionalId} son filtros opcionales. */
    List<AvailableStart> search(long specialtyId, LocalDate date, Long locationId, Long professionalId);

    record SpecialtyOption(long id, String code, String name, int durationMinutes, boolean general, boolean requiresAdminApproval) { }

    record ProfessionalOption(long id, String name, String professionalCode) { }

    record AvailableStart(long professionalId, String professionalName, long locationId, String locationCode, LocalDateTime startAt,
                          LocalDateTime endAt, int durationMinutes) { }
}
