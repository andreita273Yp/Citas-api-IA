package co.edu.fcv.citas.identity.domain;

/** Datos mínimos de registro (RF-01), ya validados y normalizados. */
public record PersonalData(String firstName, String lastName, Document document, EmailAddress email, String phone) {
    public PersonalData {
        firstName = Text.required(firstName, "Nombres", 80);
        lastName = Text.required(lastName, "Apellidos", 80);
        if (document == null) throw new IdentityException.InvalidData("Documento es obligatorio");
        if (email == null) throw new IdentityException.InvalidData("Email inválido");
        phone = Text.required(phone, "Teléfono", 30);
    }
}
