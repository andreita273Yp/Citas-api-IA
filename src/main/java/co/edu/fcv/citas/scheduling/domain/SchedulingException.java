package co.edu.fcv.citas.scheduling.domain;

/** Errores de negocio de agenda y citas; el adaptador web los traduce a 400, 403, 404 y 409. */
public abstract sealed class SchedulingException extends RuntimeException {
    protected SchedulingException(String message) { super(message); }

    /** Datos que no cumplen formato o reglas temporales (pasado, cuadrícula de 30 min). */
    public static final class InvalidData extends SchedulingException {
        public InvalidData(String message) { super(message); }
    }

    public static final class NotFound extends SchedulingException {
        public NotFound(String message) { super(message); }
    }

    /** El estado actual impide la operación: solapamiento, franja ocupada, sede no asignada, transición inválida. */
    public static final class Conflict extends SchedulingException {
        public Conflict(String message) { super(message); }
    }

    public static final class Forbidden extends SchedulingException {
        public Forbidden(String message) { super(message); }
    }
}
