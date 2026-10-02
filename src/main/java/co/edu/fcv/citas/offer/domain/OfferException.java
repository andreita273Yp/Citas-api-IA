package co.edu.fcv.citas.offer.domain;

/** Errores de negocio de la oferta de atención; el adaptador web los traduce a 400, 404 y 409. */
public abstract sealed class OfferException extends RuntimeException {
    protected OfferException(String message) { super(message); }

    public static final class InvalidData extends OfferException {
        public InvalidData(String message) { super(message); }
    }

    public static final class NotFound extends OfferException {
        public NotFound(String message) { super(message); }
    }

    public static final class Duplicate extends OfferException {
        public Duplicate(String message) { super(message); }
    }
}
