package co.edu.fcv.citas.identity.domain;

/** Validación de texto obligatorio con límite de longitud del modelo de datos. */
final class Text {
    private Text() { }

    static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IdentityException.InvalidData(field + " es obligatorio");
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) throw new IdentityException.InvalidData(field + " supera " + maxLength + " caracteres");
        return trimmed;
    }
}
