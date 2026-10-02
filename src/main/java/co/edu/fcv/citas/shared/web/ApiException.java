package co.edu.fcv.citas.shared.web;

import org.springframework.http.HttpStatus;

/** Error de adaptador web con un estado HTTP explícito; se serializa como Problem Details. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }
}
