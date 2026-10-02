package co.edu.fcv.citas.identity.adapter.in.web;

import co.edu.fcv.citas.identity.domain.IdentityException;
import co.edu.fcv.citas.shared.web.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores del dominio de identidad a los códigos del contrato (400, 401, 409). */
@RestControllerAdvice
class IdentityExceptionHandler {
    @ExceptionHandler(IdentityException.class)
    ProblemDetail identity(IdentityException e) {
        HttpStatus status = switch (e) {
            case IdentityException.InvalidData ignored -> HttpStatus.BAD_REQUEST;
            case IdentityException.Duplicate ignored -> HttpStatus.CONFLICT;
            case IdentityException.InvalidCredentials ignored -> HttpStatus.UNAUTHORIZED;
            case IdentityException.InvalidSession ignored -> HttpStatus.UNAUTHORIZED;
            case IdentityException.InvalidResetToken ignored -> HttpStatus.UNAUTHORIZED;
        };
        return ApiExceptionHandler.problem(status, e.getMessage());
    }
}
