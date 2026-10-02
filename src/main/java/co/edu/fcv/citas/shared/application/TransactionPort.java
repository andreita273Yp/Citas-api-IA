package co.edu.fcv.citas.shared.application;

import java.util.function.Supplier;

/** Ejecuta un caso de uso en una transacción, sin acoplar la aplicación a Spring. Una llamada anidada se une a la externa. */
public interface TransactionPort {
    <T> T inTransaction(Supplier<T> work);
}
