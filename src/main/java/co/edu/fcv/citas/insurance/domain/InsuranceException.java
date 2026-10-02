package co.edu.fcv.citas.insurance.domain;

/** Errores de negocio de aseguramiento; el adaptador web los traduce a 400, 404 y 409. */
public abstract sealed class InsuranceException extends RuntimeException {
    protected InsuranceException(String message) { super(message); }

    public static final class InvalidData extends InsuranceException {
        public InvalidData(String message) { super(message); }
    }

    public static final class NotFound extends InsuranceException {
        public NotFound(String message) { super(message); }
    }

    public static final class Duplicate extends InsuranceException {
        public Duplicate(String message) { super(message); }
    }
}
