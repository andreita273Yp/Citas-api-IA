package co.edu.fcv.citas.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Email normalizado (trim + minúsculas) que se compara sin distinguir mayúsculas. */
public record EmailAddress(String value) {
    private static final int MAX_LENGTH = 160;
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public EmailAddress {
        if (value == null || value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches())
            throw new IdentityException.InvalidData("Email inválido");
    }

    public static EmailAddress of(String raw) {
        return new EmailAddress(raw == null ? null : raw.trim().toLowerCase(Locale.ROOT));
    }
}
