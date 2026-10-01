package co.edu.fcv.citas.identity.domain;

import java.util.regex.Pattern;

/** Datos mínimos de registro (RF-01), ya validados y normalizados. */
public record PersonalData(String firstName, String lastName, Document document, EmailAddress email, String phone) {
    /** Dígitos con prefijo + opcional y separadores comunes; entre 7 y 15 dígitos (E.164). */
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()-]*$");

    public PersonalData {
        firstName = Text.required(firstName, "Nombres", 80);
        lastName = Text.required(lastName, "Apellidos", 80);
        if (document == null) throw new IdentityException.InvalidData("Documento es obligatorio");
        if (email == null) throw new IdentityException.InvalidData("Email inválido");
        phone = phone(phone);
    }

    public static String phone(String raw) {
        String value = Text.required(raw, "Teléfono", 30);
        long digits = value.chars().filter(Character::isDigit).count();
        if (!PHONE.matcher(value).matches() || digits < 7 || digits > 15)
            throw new IdentityException.InvalidData("Teléfono inválido: use de 7 a 15 dígitos");
        return value;
    }

    public PersonalData withPhone(String newPhone) {
        return new PersonalData(firstName, lastName, document, email, newPhone);
    }
}
