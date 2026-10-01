package co.edu.fcv.citas.insurance.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalización de códigos y nombres de catálogos configurables (EPS y planes). */
public final class CatalogText {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_-]{2,50}$");

    private CatalogText() { }

    /** MAYÚSCULAS, espacios internos como guion bajo; 2 a 50 letras, números, _ o -. */
    public static String code(String raw, int maxLength) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        if (!CODE.matcher(value).matches() || value.length() > maxLength)
            throw new InsuranceException.InvalidData("El código debe tener de 2 a " + maxLength + " letras, números, _ o -");
        return value;
    }

    public static String name(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) throw new InsuranceException.InvalidData("El nombre es obligatorio");
        String value = raw.trim();
        if (value.length() > maxLength) throw new InsuranceException.InvalidData("El nombre supera " + maxLength + " caracteres");
        return value;
    }

    public static String membership(String raw) {
        if (raw == null || raw.isBlank()) throw new InsuranceException.InvalidData("El número de afiliación es obligatorio");
        String value = raw.trim();
        if (value.length() > 80) throw new InsuranceException.InvalidData("El número de afiliación supera 80 caracteres");
        return value;
    }
}
