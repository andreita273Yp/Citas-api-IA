package co.edu.fcv.citas.identity.domain;

/** Errores de negocio de identidad; el adaptador web los traduce a códigos HTTP del contrato. */
public abstract sealed class IdentityException extends RuntimeException {
    protected IdentityException(String message) { super(message); }

    /** Datos de entrada que no cumplen las reglas (400). */
    public static final class InvalidData extends IdentityException {
        public InvalidData(String message) { super(message); }
    }

    /** Email o documento ya registrado (409). */
    public static final class Duplicate extends IdentityException {
        public Duplicate() { super("Email o documento ya registrado"); }
    }

    /** Credenciales inválidas; el mensaje no revela qué dato falló (401). */
    public static final class InvalidCredentials extends IdentityException {
        public InvalidCredentials() { super("Credenciales inválidas"); }
    }

    /** Refresh ausente, falsificado, vencido, revocado o reutilizado (401). */
    public static final class InvalidSession extends IdentityException {
        public InvalidSession() { super("Refresh inválido"); }
    }
}
