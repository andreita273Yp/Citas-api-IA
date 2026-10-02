package co.edu.fcv.citas.catalog.domain;

/** Elemento de un catálogo fijo de solo lectura. */
public record CatalogItem(long id, String code, String name) {
}
