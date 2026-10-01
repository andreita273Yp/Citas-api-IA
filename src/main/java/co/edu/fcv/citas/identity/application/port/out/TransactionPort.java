package co.edu.fcv.citas.identity.application.port.out;

import java.util.function.Supplier;

/** Ejecuta un caso de uso en una transacción, sin acoplar la aplicación a Spring. */
public interface TransactionPort {
    <T> T inTransaction(Supplier<T> work);
}
