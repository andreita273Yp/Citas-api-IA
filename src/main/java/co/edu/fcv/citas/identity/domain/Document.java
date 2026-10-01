package co.edu.fcv.citas.identity.domain;

import java.util.Locale;

/** Documento de identidad, único por tipo + número normalizados. */
public record Document(String type, String number) {
    public Document {
        type = Text.required(type, "Tipo de documento", 20).toUpperCase(Locale.ROOT);
        number = Text.required(number, "Número de documento", 40);
    }
}
