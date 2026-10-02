package co.edu.fcv.citas.offer.application.port.out;

/** Crea la identidad PROFESSIONAL (módulo de identidad) y devuelve el id del usuario. */
public interface ProfessionalAccountPort {
    long createAccount(String firstName, String lastName, String documentType, String documentNumber, String email, String phone,
                       String initialPassword);
}
