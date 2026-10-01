package co.edu.fcv.citas.offer.domain;

import java.util.List;

/** Lectura de un profesional con sus asignaciones vigentes. */
public record ProfessionalView(long id, long userId, String firstName, String lastName, String email, String phone,
                               ProfessionalCredentials credentials, boolean active, List<AssignedSpecialty> specialties,
                               List<AssignedLocation> locations) {
    public record AssignedSpecialty(long id, String code, String name, int durationMinutes, boolean primary) { }

    public record AssignedLocation(long id, String code, String name) { }
}
