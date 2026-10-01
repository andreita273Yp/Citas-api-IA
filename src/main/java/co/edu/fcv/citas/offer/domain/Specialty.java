package co.edu.fcv.citas.offer.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Especialidad del catálogo configurable (RF-06/RF-09). La duración es la única fuente para calcular slots;
 * {@code general} identifica Medicina General (aprobación automática) y no lo cambia el ADMIN.
 */
public record Specialty(Long id, String code, String name, AppointmentDuration duration, boolean general, boolean active) {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{2,50}$");

    public Specialty {
        code = normalizeCode(code);
        name = requiredName(name);
        if (duration == null) throw new OfferException.InvalidData("La duración es obligatoria");
    }

    /** Las especialidades creadas por ADMIN son especializadas: requieren aprobación administrativa (RN-03). */
    public static Specialty newSpecialized(String code, String name, Integer durationMinutes) {
        if (durationMinutes == null) throw new OfferException.InvalidData("La duración es obligatoria");
        return new Specialty(null, code, name, new AppointmentDuration(durationMinutes), false, true);
    }

    public boolean requiresAdminApproval() { return !general; }

    public Specialty update(String newName, Integer newDurationMinutes, Boolean newActive) {
        return new Specialty(id, code, newName == null ? name : newName,
                newDurationMinutes == null ? duration : new AppointmentDuration(newDurationMinutes), general,
                newActive == null ? active : newActive);
    }

    private static String normalizeCode(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        if (!CODE.matcher(value).matches()) throw new OfferException.InvalidData("El código debe tener de 2 a 50 letras, números o _");
        return value;
    }

    private static String requiredName(String raw) {
        if (raw == null || raw.isBlank()) throw new OfferException.InvalidData("El nombre es obligatorio");
        String value = raw.trim();
        if (value.length() > 150) throw new OfferException.InvalidData("El nombre supera 150 caracteres");
        return value;
    }
}
