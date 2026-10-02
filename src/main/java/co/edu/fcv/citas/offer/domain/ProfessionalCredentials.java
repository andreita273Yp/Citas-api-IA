package co.edu.fcv.citas.offer.domain;

/** Código profesional y matrícula ficticia (RF-07), ambos únicos. */
public record ProfessionalCredentials(String professionalCode, String licenseNumber) {
    public ProfessionalCredentials {
        professionalCode = required(professionalCode, "El código profesional", 40);
        licenseNumber = required(licenseNumber, "La matrícula", 80);
    }

    private static String required(String raw, String field, int max) {
        if (raw == null || raw.isBlank()) throw new OfferException.InvalidData(field + " es obligatorio");
        String value = raw.trim();
        if (value.length() > max) throw new OfferException.InvalidData(field + " supera " + max + " caracteres");
        return value;
    }
}
