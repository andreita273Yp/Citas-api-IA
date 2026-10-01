package co.edu.fcv.citas.insurance.adapter.in.web;

import co.edu.fcv.citas.insurance.domain.InsuranceException;
import co.edu.fcv.citas.shared.web.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de aseguramiento a los códigos del contrato (400, 404, 409). */
@RestControllerAdvice
class InsuranceExceptionHandler {
    @ExceptionHandler(InsuranceException.class)
    ProblemDetail insurance(InsuranceException e) {
        HttpStatus status = switch (e) {
            case InsuranceException.InvalidData ignored -> HttpStatus.BAD_REQUEST;
            case InsuranceException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case InsuranceException.Duplicate ignored -> HttpStatus.CONFLICT;
        };
        return ApiExceptionHandler.problem(status, e.getMessage());
    }
}
