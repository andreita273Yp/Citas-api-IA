package co.edu.fcv.citas.catalog.domain;

/** Sede fija del laboratorio. {@code id} se expone como texto para el cliente web. */
public record Location(String id, String code, String name, String address, String city, String department) {
}
